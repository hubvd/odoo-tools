package com.github.hubvd.odootools.actions.utils

sealed interface BranchLookup {
    operator fun invoke(value: String): BranchRef?
}

class CompositeBranchLookup(private val delegates: Collection<BranchLookup>) : BranchLookup {
    override fun invoke(value: String): BranchRef? = delegates.firstNotNullOfOrNull { it.invoke(value) }
}

class GithubBranchLookup(private val github: GithubClient) : BranchLookup {
    override fun invoke(value: String) = github.lookupBranch(value)
}

data object CommitRefBranchLookup : BranchLookup {
    override fun invoke(value: String): BranchRef? {
        val colonIndex = value.indexOf(':')
        if (colonIndex < 0) return null
        val remote = value.substring(0, colonIndex)
        val branch = value.substring(colonIndex + 1)
        val base = Regex("^(master|(?:saas-)?[\\d.]+)").find(branch)?.groups?.get(1)?.value
            ?: return null

        return BranchRef(remote, branch, base)
    }
}

data class BranchRef(val remote: String, val branch: String, val base: String)
