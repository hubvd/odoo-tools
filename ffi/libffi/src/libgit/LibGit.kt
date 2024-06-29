@file:Suppress("FunctionName", "unused", "SpellCheckingInspection")

package com.github.hubvd.odootools.ffi.libffi.libgit

import java.lang.foreign.MemorySegment
import kotlin.reflect.KClass

annotation class Repo(val index: Int)
annotation class Self(val index: Int)

@Target(AnnotationTarget.TYPE)
annotation class Out(val index: Int)

annotation class CContext(val value: KClass<out Library>)

@CContext(LibGitLibrary::class)
annotation class LibGitModule

fun main() {
    System.loadLibrary("git2")
    val git = LibGit()
    git.git_libgit2_init()
    val repo = git.git_repository_open("/home/hubert/src/test/odoo")!!
    println(repo)
    println(repo.git_repository_path())
}

@LibGitModule
interface LibGit : CModule {
    fun git_libgit2_init(): Int

    fun git_repository_open(path: String): @[Out(0)] Repository?

    companion object : CModule.Factory<LibGit>()
}

@LibGitModule
interface Repository : Struct {

    @Self(1)
    fun git_repository_head(): @[Out(0)] GitReference

    @Self(0)
    fun git_repository_path(): String

    companion object : CModule.Factory<Repository>()
}

interface RepoReference {
    var repo: MemorySegment
}

interface GitOid : RepoReference {

    @[Repo(1) Self(2)]
    fun commit_lookup(): @[Out(0)] GitCommit
}

interface GitReference : RepoReference, Struct {
    fun reference_shorthand(): String
}

interface GitObject

interface GitCommit : Struct
