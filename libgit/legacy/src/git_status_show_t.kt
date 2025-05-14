@file:Suppress("ClassName", "ktlint:standard:filename")

package com.github.hubvd.odootools.libgit.legacy

enum class git_status_show_t {
    GIT_STATUS_SHOW_INDEX_AND_WORKDIR,
    GIT_STATUS_SHOW_INDEX_ONLY,
    GIT_STATUS_SHOW_WORKDIR_ONLY,
    ;

    val value get() = ordinal
}
