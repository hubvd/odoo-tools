@file:Suppress("ClassName", "ktlint:standard:filename")

package com.github.hubvd.odootools.actions.git

import java.lang.foreign.MemoryLayout
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_LONG

object git_buff {
    val layout = MemoryLayout.structLayout(
        ADDRESS.withName("ptr"),
        JAVA_LONG.withName("reserved"),
        JAVA_LONG.withName("size"),
    ).withName("git_buff")
}
