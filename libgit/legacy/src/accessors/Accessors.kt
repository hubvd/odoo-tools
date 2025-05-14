package com.github.hubvd.odootools.libgit.legacy.accessors

import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemoryLayout.PathElement.groupElement
import java.lang.foreign.MemorySegment
import java.lang.invoke.VarHandle
import kotlin.enums.EnumEntries
import kotlin.enums.enumEntries
import kotlin.reflect.KProperty

interface NativeStruct {
    val segment: MemorySegment
}

interface NativeLayout {
    val layout: MemoryLayout
}

interface VarHandleAccessor<T> {
    fun get(): T
    fun set(value: T)
}

class StringVarHandleAccessor(private val nativeStruct: NativeStruct, private val handle: VarHandle) {
    fun set(arena: Arena, value: String) {
        val segment = arena.allocateFrom(value)
        handle.set(nativeStruct.segment, 0L, segment)
    }

    fun get(): String? {
        val res = handle.get(nativeStruct.segment, 0L) as MemorySegment
        if (res == MemorySegment.NULL) return null
        return res.reinterpret(Long.MAX_VALUE).getString(0)
    }
}

class EnumOrdinalVarHandleAccessor<T : Enum<T>>(
    private val nativeStruct: NativeStruct,
    private val handle: VarHandle,
    private val entries: EnumEntries<T>,
) {
    fun get(): T {
        val ordinal = handle.get(nativeStruct.segment, 0) as Int
        return entries[ordinal]
    }
}

fun <T> MemoryLayout.field() = VarHandleDelegate<T>(this)

fun MemoryLayout.string() = StringVarHandleDelegate(this)

inline fun <reified T : Enum<T>> MemoryLayout.ordinalEnum() = EnumOrdinalVarHandleDelegate(this, enumEntries<T>())

class EnumOrdinalVarHandleDelegate<T : Enum<T>>(
    private val layout: MemoryLayout,
    private val entries: EnumEntries<T>,
) {
    operator fun getValue(thisRef: NativeStruct, property: KProperty<*>): EnumOrdinalVarHandleAccessor<T> {
        val handle = layout.varHandle(groupElement(property.name))
        return EnumOrdinalVarHandleAccessor(thisRef, handle, entries)
    }
}

class StringVarHandleDelegate(private val layout: MemoryLayout) {
    operator fun getValue(thisRef: NativeStruct, property: KProperty<*>): StringVarHandleAccessor {
        val handle = layout.varHandle(groupElement(property.name))
        return StringVarHandleAccessor(thisRef, handle)
    }
}

class VarHandleDelegate<T>(private val layout: MemoryLayout) {
    operator fun getValue(thisRef: NativeStruct, property: KProperty<*>): VarHandleAccessor<T> {
        val handle = layout.varHandle(groupElement(property.name))
        return object : VarHandleAccessor<T> {
            override fun get(): T = handle.get(thisRef.segment, 0) as T

            override fun set(value: T) {
                handle.set(thisRef.segment, 0, value)
            }
        }
    }
}
