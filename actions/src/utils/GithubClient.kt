package com.github.hubvd.odootools.actions.utils

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.hubvd.odootools.actions.Secret
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.http4k.core.*
import org.http4k.filter.ClientFilters
import org.http4k.filter.RequestFilters

class GithubClient(githubApiKey: Secret, httpHandler: HttpHandler) {
    private val client = ClientFilters.SetHostFrom(Uri.of("https://api.github.com"))
        .then(RequestFilters.SetHeader("Content-Type", "application/vnd.github+json"))
        .then(ClientFilters.BearerAuth { githubApiKey.value })
        .then(httpHandler)

    fun lookupBranch(value: String): BranchRef? {
        val (_, org, repo, id) = PULL_REQUEST_REGEX.find(value)?.groupValues
            ?: return null

        val response = client(Request(Method.GET, "/repos/$org/$repo/pulls/$id"))
        if (!response.status.successful) return null
        val pr = Json.decodeFromString(JsonObject.serializer(), response.bodyString())
        return BranchRef(
            remote = pr["head"]!!.jsonObject["user"]!!.jsonObject["login"]!!.jsonPrimitive.content,
            branch = pr["head"]!!.jsonObject["ref"]!!.jsonPrimitive.content,
            base = pr["base"]!!.jsonObject["ref"]!!.jsonPrimitive.content,
        )
    }

    fun findPullRequestsInvolving(username: String): Either<Response, List<PullRequest>> {
        val query = this.javaClass.getResource("/query.graphql")!!.readText()
            .replace("<INVOLVES>", username)

        val request = Request(Method.POST, "/graphql")
            .body(Json.encodeToString(JsonObject.serializer(), buildJsonObject { put("query", query) }))
        val response = client(request)

        if (!response.status.successful) {
            return response.left()
        }
        val jsonPrs = Json.decodeFromString(
            JsonObject.serializer(),
            response.bodyString(),
        )["data"]!!.jsonObject["results"]!!.jsonObject["prs"]!!.jsonArray
        return Json.decodeFromJsonElement(ListSerializer(PullRequest.serializer()), jsonPrs).right()
    }

    companion object {
        private val PULL_REQUEST_REGEX = Regex("^(?:https://)?github.com/(\\w+)/(\\w+)/pull/(\\d+)")
    }
}

fun PullRequest.state() = when {
    checks
        .filterNot { it.context == "ci/codeowner" }
        .any { it.state == CheckState.FAILURE || it.state == CheckState.ERROR } -> {
        CheckState.FAILURE
    }

    checks.isEmpty() || checks.any { it.state == CheckState.PENDING } -> {
        CheckState.PENDING
    }

    else -> {
        CheckState.SUCCESS
    }
}

@Serializable
data class PullRequest(
    val headRefName: String,
    val title: String,
    val url: String,
    val isDraft: Boolean,
    val mergeable: Mergeable,
    @SerialName("commits")
    @Serializable(with = CheckSerializer::class) val checks: List<Check>,
    @Serializable(with = TimelineItemsSerializer::class) val timelineItems: List<TimelineItem>,
) {
    @JvmName("computeIsDraft")
    fun isDraft(): Boolean = isDraft || DRAFT_RE.matches(title)

    fun normalizedTitle() = buildString {
        if (isDraft()) {
            append("[DRAFT]")
            append(title.replace(DRAFT_RE, "\$1"))
        } else {
            append(title)
        }
    }

    fun isInConflict(): Boolean {
        if (mergeable == Mergeable.CONFLICTING) return true
        var isInConflict = false
        for (timelineItem in timelineItems) {
            when (timelineItem) {
                is TimelineItem.IssueComment -> {
                    if (timelineItem.author == "fw-bot" && timelineItem.body.contains("CONFLICT")) {
                        isInConflict = true
                    }
                }

                is TimelineItem.PullRequestCommit -> {
                    isInConflict = false
                }
            }
        }
        return isInConflict
    }

    companion object {
        private val DRAFT_RE = """^\[(?:WIP|DRAFT)](.*)""".toRegex()
    }
}

@Serializable
enum class Mergeable { CONFLICTING, MERGEABLE, UNKNOWN }

@Serializable(with = TimelineItemSerializer::class)
sealed class TimelineItem {
    data class IssueComment(val body: String, val author: String) : TimelineItem()
    data class PullRequestCommit(val title: String) : TimelineItem()
}

@Serializable
data class Check(val state: CheckState, val targetUrl: String, val context: String)

@Serializable
data class Comment(val body: String, val login: String)

enum class CheckState { FAILURE, SUCCESS, PENDING, ERROR }

private object TimelineItemsSerializer : JsonTransformingSerializer<List<TimelineItem>>(
    ListSerializer(TimelineItem.serializer()),
) {
    override fun transformDeserialize(element: JsonElement): JsonElement = element.jsonObject["nodes"]!!
}

private object TimelineItemSerializer : KSerializer<TimelineItem> {
    override fun deserialize(decoder: Decoder): TimelineItem {
        val input = decoder as JsonDecoder
        val element = input.decodeJsonElement().jsonObject
        return when (element["type"]!!.jsonPrimitive.content) {
            "PullRequestCommit" -> {
                TimelineItem.PullRequestCommit(
                    title = element["commit"]!!.jsonObject["title"]!!.jsonPrimitive.content,
                )
            }

            "IssueComment" -> {
                TimelineItem.IssueComment(
                    body = element["body"]!!.jsonPrimitive.content,
                    author = element["author"]!!.jsonObject["login"]!!.jsonPrimitive.content,
                )
            }

            else -> {
                throw IllegalArgumentException(element["type"]!!.jsonPrimitive.content)
            }
        }
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TimelineItem") {
        element<String>("type")
    }
    override fun serialize(encoder: Encoder, value: TimelineItem): Unit = throw UnsupportedOperationException()
}

private object CheckSerializer : JsonTransformingSerializer<List<Check>>(ListSerializer(Check.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val nodes = element.jsonObject["nodes"]!!.jsonArray
        return if (nodes.isEmpty()) {
            nodes
        } else {
            when (val status = nodes[0].jsonObject["commit"]?.jsonObject?.get("status")) {
                is JsonObject -> status.jsonObject["contexts"] ?: nodes
                else -> JsonArray(emptyList())
            }
        }
    }
}
