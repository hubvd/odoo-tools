@file:Suppress("ClassName", "ktlint:standard:filename")

package com.github.hubvd.odootools.actions.git

import java.lang.foreign.MemoryLayout
import java.lang.foreign.StructLayout
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT

object git_error {
    val layout: StructLayout = MemoryLayout.structLayout(
        ADDRESS.withName("message"),
        JAVA_INT.withName("klass"),
        MemoryLayout.paddingLayout(4),
    ).withName("git_error")
}
