@file:Suppress("ClassName", "ktlint:standard:filename")

package com.github.hubvd.odootools.libgit.legacy

enum class git_object_t(val value: Int) {
    GIT_OBJECT_ANY(-2),
    GIT_OBJECT_INVALID(-1),
    GIT_OBJECT_COMMIT(1),
    GIT_OBJECT_TREE(2),
    GIT_OBJECT_BLOB(3),
    GIT_OBJECT_TAG(4),
    GIT_OBJECT_OFS_DELTA(6),
    GIT_OBJECT_REF_DELTA(7),
}
