@file:Suppress("ClassName", "ktlint:standard:filename")

package com.github.hubvd.odootools.ffi.libffi.test

import com.github.hubvd.odootools.ffi.libffi.Struct
import com.github.hubvd.odootools.ffi.libffi.StructProxyDescriptor
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.StructLayout
import java.lang.foreign.ValueLayout
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT
import java.util.*

interface git_error : Struct {
    var message: String?
    var klass: Int

    companion object : StructProxyDescriptor<git_error>() {
        override val layout: StructLayout = MemoryLayout.structLayout(
            ADDRESS.withName("message"),
            JAVA_INT.withName("klass"),
            MemoryLayout.paddingLayout(4),
        ).withName("git_error")
    }
}

fun main() {
    val fmt = HexFormat.of()
    val gitError = git_error(Arena.global())
    println(gitError.message)
    println(fmt.formatHex(gitError.segment.toArray(ValueLayout.JAVA_BYTE)))
    gitError.message = "test"
    println(gitError.message)
    println(fmt.formatHex(gitError.segment.toArray(ValueLayout.JAVA_BYTE)))
    gitError.klass = Int.MAX_VALUE
    println(fmt.formatHex(gitError.segment.toArray(ValueLayout.JAVA_BYTE)))
    gitError.message = null
    println(fmt.formatHex(gitError.segment.toArray(ValueLayout.JAVA_BYTE)))
    println(gitError.message)
}
