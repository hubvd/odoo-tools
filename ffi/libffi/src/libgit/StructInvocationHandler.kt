package com.github.hubvd.odootools.ffi.libffi.libgit

import com.github.hubvd.odootools.ffi.libffi.kotlinProperty
import java.lang.foreign.Arena
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.reflect.jvm.kotlinFunction

class StructInvocationHandler(
    private val linker: Linker,
    private val lookup: SymbolLookup,
    private val arena: Arena,
    private val library: Library,
    internal val segment: MemorySegment? = null,
) : InvocationHandler {
    fun copy(segment: MemorySegment? = null): StructInvocationHandler {
        return StructInvocationHandler(linker, lookup, arena, library, segment)
    }

    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
        val fn = method.kotlinFunction!!

        if (fn == Any::toString) {
            return "()"
        }

        val property = method.kotlinProperty
        if (property != null) {
            val (prop, getter) = property
            return when {
                prop.name == "segment" && getter -> segment
                else -> TODO()
            }
        }

        val segment = lookup.find(library.methodName(fn)).get()
        val descriptor = library.methodToFunctionDescriptor(fn)
        val handle = linker.downcallHandle(segment, descriptor)
        val arguments = library.dispatchArguments(proxy, fn, args)
        val result = handle.invokeWithArguments(arguments)
        val actualResult = library.handleResult(fn, arguments, result)
        return actualResult
    }
}
