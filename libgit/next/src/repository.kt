@file:Suppress("FunctionName", "ktlint:standard:filename")

package com.github.hubvd.odootools.libgit

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT

private val linker = Linker.nativeLinker()!!
private val lookup = SymbolLookup.loaderLookup()

class LibGit2Exception : RuntimeException()

val git_repository_open_handle = linker.downcallHandle(
    lookup.find("git_repository_open").get(),
    FunctionDescriptor.of(
        JAVA_INT,
        ADDRESS,
        ADDRESS,
    )!!,
)!!

val git_repository_free_handle = linker.downcallHandle(
    lookup.find("git_repository_free").get(),
    FunctionDescriptor.ofVoid(
        ADDRESS,
    ),
)!!

fun git_repository_open(path: String): GitRepository? = Arena.ofConfined().use { allocator ->
    val out = allocator.allocate(ADDRESS)
    val result = git_repository_open_handle.invokeExact(out, allocator.allocateFrom(path)) as Int
    if (result != 0) {
        throw LibGit2Exception()
    }
    val address = out.get(ADDRESS, 0) as MemorySegment
    address.takeIf { it != MemorySegment.NULL }?.let { GitRepository(it) }
}

class GitRepository(self: MemorySegment) : Pointer, AutoCloseable {

    override val self: MemorySegment

    private val arena: Arena = Arena.ofConfined()

    init {
        this.self = self.reinterpret(this.arena, ::free)
    }

    private fun free(address: MemorySegment) {
        git_repository_free_handle.invokeExact(address)
    }

    override fun close() {
        arena.close()
    }
}

class R(override val self: MemorySegment) : OpaquePointer

interface OpaquePointer : Pointer

interface Pointer {
    val self: MemorySegment
}

fun main() {
    val repo = git_repository_open("")
}
