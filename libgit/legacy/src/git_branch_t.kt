@file:Suppress("ClassName", "ktlint:standard:filename")

package com.github.hubvd.odootools.libgit.legacy

enum class git_branch_t(val value: Int) {
    GIT_BRANCH_LOCAL(1),
    GIT_BRANCH_REMOTE(2),
    GIT_BRANCH_ALL(1 and 2),
}
