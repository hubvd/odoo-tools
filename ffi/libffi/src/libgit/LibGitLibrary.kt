package com.github.hubvd.odootools.ffi.libffi.libgit

import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType

class LibGitLibrary : Library {
    override fun methodToFunctionDescriptor(method: KFunction<*>): FunctionDescriptor {
        val out = method.returnType.annotations.filterIsInstance<Out>().firstOrNull()?.index

        val returnType = when {
            out != null -> ValueLayout.JAVA_INT
            method.returnType.classifier == String::class -> ValueLayout.ADDRESS
            method.returnType.classifier == Int::class -> ValueLayout.JAVA_INT
            else -> TODO(method.returnType.toString())
        }

        val self = method.annotations.filterIsInstance<Self>().firstOrNull()?.index

        val arguments = buildList<MemoryLayout> {
            if (out != null) {
                // out is the first argument in libgit // FIXME
                add(ValueLayout.ADDRESS)
            }

            // FIXME
            if (self == 0) {
                add(ValueLayout.ADDRESS)
            }

            method.parameters.dropWhile { it.kind != KParameter.Kind.VALUE }.forEach { param: KParameter ->
                add(typeToLayout(param.type))
            }
        }

        return FunctionDescriptor.of(
            returnType,
            *arguments.toTypedArray(),
        ).also { println(it) }
    }

    private fun typeToLayout(type: KType): MemoryLayout = when (type.classifier) {
        Int::class -> ValueLayout.JAVA_INT
        Long::class -> ValueLayout.JAVA_LONG
        Boolean::class -> ValueLayout.JAVA_BOOLEAN
        MemorySegment::class, String::class -> ValueLayout.ADDRESS
        else -> TODO(type.toString())
    }

    context(StructInvocationHandler)
    override fun dispatchArguments(proxy: Any, method: KFunction<*>, arguments: Array<out Any?>?): List<Any?> {
        val self = method.annotations.filterIsInstance<Self>().firstOrNull()?.index
        val out = method.returnType.annotations.filterIsInstance<Out>().firstOrNull()?.index

        val output = buildList {
            if (out != null) {
                add(arena.allocate(ValueLayout.ADDRESS))
            }

            // FIXME
            if (self == 0) {
                add(segment!!)
            }

            arguments?.forEach {
                when {
                    it is String -> add(arena.allocateFrom(it))
                    else -> TODO()
                }
            }
        }

        return output
    }

    context(StructInvocationHandler)
    override fun handleResult(method: KFunction<*>, arguments: List<Any?>, returnedValue: Any?): Any? {
        if (returnedValue is Int) {
            println("return:$returnedValue")
        }

        val out = method.returnType.annotations.filterIsInstance<Out>().firstOrNull()?.index

        return when {
            method.returnType.classifier == Int::class && returnedValue is Int -> returnedValue
            method.returnType.classifier == String::class -> {
                if (returnedValue == null) {
                    null
                } else if (returnedValue is MemorySegment) {
                    if (returnedValue == MemorySegment.NULL) {
                        null
                    } else {
                        returnedValue.reinterpret(Long.MAX_VALUE).getString(0)
                    }
                } else {
                    TODO()
                }
            }

            out != null -> {
                val value = (arguments.first() as MemorySegment).get(ValueLayout.ADDRESS, 0L)
                if (value == MemorySegment.NULL) return null
                val type = method.returnType.classifier as KClass<CModule>
                CModule.Factory.proxy(
                    copy(value),
                    type,
                )
            }

            else -> TODO()
        }
    }
}
