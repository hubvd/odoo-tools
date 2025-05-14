package com.github.hubvd.odootools.libgit.legacy

import com.github.hubvd.odootools.libgit.legacy.accessors.NativeLayout
import com.github.hubvd.odootools.libgit.legacy.accessors.NativeStruct
import com.github.hubvd.odootools.libgit.legacy.accessors.ordinalEnum
import com.github.hubvd.odootools.libgit.legacy.accessors.string
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT

class GitError(override val segment: MemorySegment) : NativeStruct {
    val klass by layout.ordinalEnum<GitErrorT>()
    val message by layout.string()

    companion object : NativeLayout {
        override val layout: MemoryLayout = MemoryLayout.structLayout(
            ADDRESS.withName("message"),
            JAVA_INT.withName("klass"),
            MemoryLayout.paddingLayout(4),
        ).withName("git_error")

    }
}
