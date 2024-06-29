package com.github.hubvd.odootools.ffi.libffi.libgit

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.SymbolLookup
import kotlin.reflect.KFunction

interface Library {
    val linker: Linker
        get() = Linker.nativeLinker()

    val arena: Arena
        get() = Arena.ofConfined()

    val symbolLookup: SymbolLookup
        get() = SymbolLookup.loaderLookup()

    fun methodName(method: KFunction<*>): String = method.name

    fun methodToFunctionDescriptor(method: KFunction<*>): FunctionDescriptor

    context(StructInvocationHandler)
    fun dispatchArguments(
        proxy: Any,
        method: KFunction<*>,
        arguments: Array<out Any?>?,
    ): List<Any?>

    context(StructInvocationHandler)
    fun handleResult(
        method: KFunction<*>,
        arguments: List<Any?>,
        returnedValue: Any?,
    ): Any?
}
