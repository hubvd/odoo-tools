package com.github.hubvd.odootools.actions

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.http4k.core.*
import org.http4k.filter.ClientFilters
import org.http4k.filter.RequestFilters

sealed interface BranchLookup {
    operator fun invoke(value: String): BranchRef?
}

class CompositeBranchLookup(private val delegates: List<BranchLookup>) : BranchLookup {
    override fun invoke(value: String): BranchRef? = delegates.firstNotNullOfOrNull { it.invoke(value) }
}

class GithubBranchLookup(
    githubApiKey: Secret,
    httpHandler: HttpHandler,
) : BranchLookup {

    private val client = ClientFilters.SetHostFrom(Uri.of("https://api.github.com"))
        .then(RequestFilters.SetHeader("Content-Type", "application/vnd.github+json"))
        .then(ClientFilters.BearerAuth { githubApiKey.value })
        .then(httpHandler)

    private val pullRequestRegex = Regex("^(?:https://)?github.com/(\\w+)/(\\w+)/pull/(\\d+)")
    override fun invoke(value: String): BranchRef? {
        val (_, org, repo, id) = pullRequestRegex.find(value)?.groupValues
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
}

data object CommitRefBranchLookup : BranchLookup {
    override fun invoke(value: String): BranchRef? {
        val remote = value.substringBefore(':')
        val branch = value.substringAfter(':')
        val base = Regex("^(master|(?:saas-)?[\\d.]+)").find(branch)?.groups?.get(1)?.value
            ?: return null

        return BranchRef(remote, branch, base)
    }
}

data class BranchRef(val remote: String, val branch: String, val base: String)
