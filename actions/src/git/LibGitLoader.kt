package com.github.hubvd.odootools.actions.git

import java.lang.foreign.Arena
import java.lang.foreign.Linker
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.reflect.Proxy

object LibGitLoader {

    init {
        System.loadLibrary("git2")
    }

    private val linker = Linker.nativeLinker()
    private val lookup = SymbolLookup.loaderLookup()

    val proxy = createProxy()

    private fun createProxy(): LibGit {
        val proxy = Proxy.newProxyInstance(
            this.javaClass.classLoader,
            arrayOf(LibGit::class.java),
            LibGitInvocationHandler(lookup, linker),
        ) as LibGit

        Arena.ofConfined().use {
            val major = it.allocate(ValueLayout.ADDRESS)
            val minor = it.allocate(ValueLayout.ADDRESS)
            val revision = it.allocate(ValueLayout.ADDRESS)
            proxy.libgit2_version(major, minor, revision)

            if (
                major.get(ValueLayout.JAVA_INT, 0) != 1 ||
                minor.get(ValueLayout.JAVA_INT, 0) != 8
            ) {
                throw UnsatisfiedLinkError("Expected libgit 1.8.*")
            }
        }

        proxy.libgit2_init()
        return proxy
    }
}
