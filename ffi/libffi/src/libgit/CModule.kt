package com.github.hubvd.odootools.ffi.libffi.libgit

import com.github.hubvd.odootools.ffi.libffi.TypeReference
import java.lang.foreign.Arena
import java.lang.foreign.Linker
import java.lang.foreign.SymbolLookup
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

interface CModule {

    @Suppress("UNCHECKED_CAST")
    abstract class Factory<T : CModule> : TypeReference<T>() {

        operator fun invoke(): T {
            val tClass = referencedKotlinType.classifier as KClass<T>
            val cContext = tClass.annotations.firstNotNullOf {
                when {
                    it is CContext -> it
                    else -> it.annotationClass.annotations.filterIsInstance<CContext>().firstOrNull()
                }
            }

            val library = cContext.value
            val lib = library.primaryConstructor!!.call()

            return unaddressed(
                lib.arena,
                lib.linker,
                lib.symbolLookup,
                lib,
                referencedKotlinType.classifier as KClass<T>,
            )
        }

        companion object {

            fun <T : CModule> proxy(invocationHandler: StructInvocationHandler, clazz: KClass<T>): T {
                val proxy = Proxy.newProxyInstance(
                    this::class.java.classLoader,
                    arrayOf(clazz.java),
                    invocationHandler,
                ) as T
                return proxy
            }

            fun <T : CModule> unaddressed(
                arena: Arena,
                linker: Linker,
                lookup: SymbolLookup,
                library: Library,
                clazz: KClass<T>,
            ): T {
                val handler = StructInvocationHandler(linker, lookup, arena, library)
                return proxy(handler, clazz)
            }
        }
    }
}
