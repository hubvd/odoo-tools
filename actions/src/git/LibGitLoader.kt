package com.github.hubvd.odootools.actions.git

import java.lang.foreign.Linker
import java.lang.foreign.SymbolLookup
import java.lang.reflect.Proxy

object LibGitLoader {

    init {
        System.loadLibrary("git2")
    }

    private val linker = Linker.nativeLinker()
    private val lookup = SymbolLookup.loaderLookup()

    val proxy = (
        Proxy.newProxyInstance(
            this.javaClass.classLoader,
            arrayOf(LibGit::class.java),
            LibGitInvocationHandler(lookup, linker),
        ) as LibGit
        ).also { it.libgit2_init() }
}
