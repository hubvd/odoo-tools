package com.github.hubvd.odootools.actions.utils

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.hubvd.odootools.actions.Secret
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import org.http4k.core.*
import org.http4k.filter.ClientFilters
import org.http4k.filter.RequestFilters
import org.http4k.filter.ResponseFilters

class GithubClient(githubApiKey: Secret, httpHandler: HttpHandler) {
    private val client = ClientFilters.SetHostFrom(Uri.of("https://api.github.com"))
        .then(RequestFilters.SetHeader("Content-Type", "application/vnd.github+json"))
        .then(ClientFilters.BearerAuth { githubApiKey.value })
        .then(ResponseFilters.GunZip())
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

    fun findPullRequests(
        username: String?,
        open: Boolean = true,
        title: String? = null,
    ): Either<Response, List<PullRequest>> {
        val query = buildList {
            add("org:odoo")
            add("type:pr")
            username?.let {
                add("involves:$it")
            }
            if (open) {
                add("state:open")
            } else {
                add("state:closed")
            }
            title?.let {
                add(it)
            }
        }.joinToString(" ")

        val body = this.javaClass.getResource("/query.graphql")!!.readText()
            .replace("<QUERY>", query)

        val request = Request(Method.POST, "/graphql")
            .body(Json.encodeToString(JsonObject.serializer(), buildJsonObject { put("query", body) }))
        val response = client(request)

        if (!response.status.successful) {
            return response.left()
        }
        val jsonBody = Json.decodeFromString(
            JsonObject.serializer(),
            response.bodyString(),
        )
        val jsonPrs = jsonBody["data"]?.jsonObject?.get("results")?.jsonObject?.get("prs")?.jsonArray
            ?: return response.left()
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

fun PullRequest.parent(): Int? = body.lineSequence()
    .filter { it.startsWith("Forward-Port-Of:") }
    .lastOrNull()
    ?.removePrefix("Forward-Port-Of: #")
    ?.toIntOrNull()

@Serializable
data class PullRequest(
    val id: Int,
    val headRefName: String,
    val baseRefName: String,
    val title: String,
    val url: String,
    val isDraft: Boolean,
    val mergeable: Mergeable,
    val body: String,
    @SerialName("commits")
    @Serializable(with = CheckSerializer::class) val checks: List<Check>,
    @Serializable(with = LabelSerializer::class) val labels: List<String>,
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

    fun isInConflict() = mergeable == Mergeable.CONFLICTING || labels.contains("conflict")

    companion object {
        private val DRAFT_RE = """^\[(?:WIP|DRAFT)](.*)""".toRegex()
    }
}

@Serializable
enum class Mergeable { CONFLICTING, MERGEABLE, UNKNOWN }

@Serializable
data class Check(val state: CheckState, val targetUrl: String, val context: String)

enum class CheckState { FAILURE, SUCCESS, PENDING, ERROR }

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

private object LabelSerializer : JsonTransformingSerializer<List<String>>(ListSerializer(String.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val nodes = element.jsonObject["nodes"]!!.jsonArray
        return JsonArray(nodes.map { it.jsonObject["name"]!! })
    }
}
