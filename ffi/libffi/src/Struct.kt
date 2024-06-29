package com.github.hubvd.odootools.ffi.libffi

import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout.PathElement
import java.lang.foreign.MemorySegment
import java.lang.foreign.StructLayout
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter

interface Struct {
    val segment: MemorySegment
}

abstract class StructProxyDescriptor<T : Struct> : TypeReference<T>() {
    abstract val layout: StructLayout

    operator fun invoke(arena: Arena): T {
        val segment = arena.allocate(layout.byteSize())

        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            this.javaClass.classLoader,
            arrayOf(referencedType as Class<*>),
            StructInvocationHandler(segment, layout, arena),
        ) as T
    }
}

class StructInvocationHandler(
    private val segment: MemorySegment,
    private val layout: StructLayout,
    private val arena: Arena,
) :
    InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
        if (method == Struct::segment.javaGetter) {
            return segment
        }

        val (property, getter) = method.kotlinProperty ?: TODO(method.name)

        if (getter) {
            val handle = layout.varHandle(PathElement.groupElement(property.name))
            val value = handle.get(segment, 0L)

            if (method.returnType == String::class.java) {
                value as MemorySegment
                if (value == MemorySegment.NULL) {
                    return null
                }
                return value.reinterpret(Long.MAX_VALUE).getString(0)
            }

            return value
        } else {
            val handle = layout.varHandle(PathElement.groupElement(property.name))
            val value = args!![0]
            val converted = if (value != null && value.javaClass == String::class.java) {
                value as String?
                arena.allocateFrom(value)
            } else {
                value ?: MemorySegment.NULL
            }

            handle.set(segment, 0L, converted)
            return null
        }
    }
}

internal val Method.kotlinProperty: Pair<KProperty1<*, *>, Boolean>?
    get() {
        for (prop in declaringClass.kotlin.declaredMemberProperties) {
            if (prop.javaGetter == this) {
                return Pair(prop, true)
            }
            if (prop is KMutableProperty<*> && prop.javaSetter == this) {
                return Pair(prop, false)
            }
        }
        return null
    }
