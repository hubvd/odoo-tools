@file:Suppress("FunctionName", "unused", "SpellCheckingInspection")

package com.github.hubvd.odootools.actions.git

import java.lang.foreign.MemorySegment

annotation class LibGitNoError

data class LibGitError(val code: Int, val klazz: Int, override val message: String) : RuntimeException()

interface LibGit {
    @LibGitNoError
    fun libgit2_init(): Int

    fun object_short_id(out: MemorySegment, obj: MemorySegment): Int

    fun repository_open(out: MemorySegment, path: MemorySegment): Int
    fun repository_free(memorySegment: MemorySegment)
    fun repository_head(out: MemorySegment, repo: MemorySegment): Int
    fun repository_set_head(repo: MemorySegment, refName: MemorySegment): Int

    fun reference_peel(out: MemorySegment, ref: MemorySegment, type: Int): Int
    fun reference_is_branch(ref: MemorySegment): Boolean
    fun reference_shorthand(ref: MemorySegment): MemorySegment
    fun reference_free(memorySegment: MemorySegment)
    fun reference_name(ref: MemorySegment): MemorySegment
    fun reference_target(ref: MemorySegment): MemorySegment

    fun branch_create(
        out: MemorySegment,
        repo: MemorySegment,
        branchName: MemorySegment,
        target: MemorySegment,
        force: Boolean,
    ): Int
    fun branch_lookup(out: MemorySegment, repo: MemorySegment, branchName: MemorySegment, branchType: Int): Int
    fun branch_upstream(out: MemorySegment, branch: MemorySegment): Int
    fun branch_name(out: MemorySegment, ref: MemorySegment): Int

    fun commit_lookup(commit: MemorySegment, repo: MemorySegment, id: MemorySegment): Int
    fun commit_free(memorySegment: MemorySegment)
    fun commit_id(commit: MemorySegment): MemorySegment

    fun graph_ahead_behind(
        ahead: MemorySegment,
        behind: MemorySegment,
        repo: MemorySegment,
        local: MemorySegment,
        upstream: MemorySegment,
    ): Int

    fun checkout_tree(repo: MemorySegment, treeish: MemorySegment, opts: MemorySegment): Int

    fun remote_list(out: MemorySegment, repo: MemorySegment): Int
    fun remote_lookup(out: MemorySegment, repo: MemorySegment, name: MemorySegment): Int
    fun remote_free(memorySegment: MemorySegment)
    fun remote_url(remote: MemorySegment): MemorySegment
    fun remote_pushurl(remote: MemorySegment): MemorySegment
    fun remote_name(remote: MemorySegment): MemorySegment

    fun status_options_init(opts: MemorySegment, version: Int = 1): Int
    fun status_list_new(out: MemorySegment, repo: MemorySegment, opts: MemorySegment): Int
    fun status_list_free(memorySegment: MemorySegment)
    fun status_list_entrycount(statusList: MemorySegment): Long

    fun strarray_dispose(memorySegment: MemorySegment)
}
