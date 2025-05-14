@file:Suppress("FunctionName", "unused", "SpellCheckingInspection")

package com.github.hubvd.odootools.libgit.legacy

import java.lang.foreign.MemorySegment

data class LibGitError(
    val code: GitErrorCode,
    val klazz: GitErrorT,
    override val message: String,
) : RuntimeException()

interface LibGit {
    fun libgit2_init(): Int
    fun libgit2_version(major: MemorySegment, minor: MemorySegment, rev: MemorySegment)

    fun object_short_id(out: MemorySegment, obj: MemorySegment)

    fun repository_open(out: MemorySegment, path: MemorySegment)
    fun repository_free(memorySegment: MemorySegment)
    fun repository_head(out: MemorySegment, repo: MemorySegment)
    fun repository_set_head(repo: MemorySegment, refName: MemorySegment)
    fun repository_path(repo: MemorySegment): MemorySegment
    fun repository_state(repo: MemorySegment): Int

    fun reference_dwim(out: MemorySegment, repo: MemorySegment, shorthand: MemorySegment)
    fun reference_peel(out: MemorySegment, ref: MemorySegment, type: Int)
    fun reference_is_branch(ref: MemorySegment): Boolean
    fun reference_shorthand(ref: MemorySegment): MemorySegment
    fun reference_free(memorySegment: MemorySegment)
    fun reference_name(ref: MemorySegment): MemorySegment
    fun reference_target(ref: MemorySegment): MemorySegment
    fun reference_set_target(out: MemorySegment, ref: MemorySegment, id: MemorySegment, logMessage: MemorySegment)

    fun revparse_single(out: MemorySegment, repo: MemorySegment, spec: MemorySegment)

    fun branch_create(
        out: MemorySegment,
        repo: MemorySegment,
        branchName: MemorySegment,
        target: MemorySegment,
        force: Boolean,
    )

    fun branch_lookup(out: MemorySegment, repo: MemorySegment, branchName: MemorySegment, branchType: Int)
    fun branch_upstream(out: MemorySegment, branch: MemorySegment)
    fun branch_name(out: MemorySegment, ref: MemorySegment)

    fun commit_lookup(commit: MemorySegment, repo: MemorySegment, id: MemorySegment)
    fun commit_free(memorySegment: MemorySegment)
    fun commit_id(commit: MemorySegment): MemorySegment

    fun oid_equal(a: MemorySegment, b: MemorySegment): Boolean
    fun object_id(obj: MemorySegment): MemorySegment
    fun oid_tostr_s(oid: MemorySegment): MemorySegment

    fun graph_ahead_behind(
        ahead: MemorySegment,
        behind: MemorySegment,
        repo: MemorySegment,
        local: MemorySegment,
        upstream: MemorySegment,
    )

    fun checkout_tree(repo: MemorySegment, treeish: MemorySegment, opts: MemorySegment)

    fun remote_list(out: MemorySegment, repo: MemorySegment)
    fun remote_lookup(out: MemorySegment, repo: MemorySegment, name: MemorySegment)
    fun remote_free(memorySegment: MemorySegment)
    fun remote_url(remote: MemorySegment): MemorySegment
    fun remote_pushurl(remote: MemorySegment): MemorySegment
    fun remote_name(remote: MemorySegment): MemorySegment

    fun status_options_init(opts: MemorySegment, version: Int = 1)
    fun status_list_new(out: MemorySegment, repo: MemorySegment, opts: MemorySegment)
    fun status_list_free(memorySegment: MemorySegment)
    fun status_list_entrycount(statusList: MemorySegment): Long

    fun strarray_dispose(memorySegment: MemorySegment)
}
