@file:Suppress("ClassName", "ktlint:standard:filename")

package com.github.hubvd.odootools.libgit.legacy

import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemoryLayout.PathElement.groupElement
import java.lang.foreign.MemorySegment
import java.lang.foreign.StructLayout
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_LONG

object git_strarray {
    val layout: StructLayout = MemoryLayout.structLayout(
        ADDRESS.withName("strings"),
        JAVA_LONG.withName("count"),
    ).withName("git_strarray")

    private val countHandle = layout.varHandle(groupElement("count"))!!
    private val stringsHandle = layout.varHandle(groupElement("strings"))!!

    fun read(strArray: MemorySegment): Array<String> {
        val count = countHandle.get(strArray) as Long
        val strings = (stringsHandle.get(strArray) as MemorySegment).reinterpret(
            ADDRESS.byteSize() * count,
        )
        return Array(count.toInt()) {
            strings.getAtIndex(ADDRESS, it.toLong()).reinterpret(Long.MAX_VALUE).getString(0)
        }
    }

    fun from(arena: Arena, values: Array<String>): MemorySegment {
        val strArray = arena.allocate(layout)
        val strings = arena.allocate(ADDRESS.byteSize() * values.size)
        values.forEachIndexed { index, s ->
            strings.setAtIndex(ADDRESS, index.toLong(), arena.allocateFrom(s))
        }
        stringsHandle.set(strArray, 0, strings)
        countHandle.set(strArray, 0, values.size)
        return strArray
    }
}
