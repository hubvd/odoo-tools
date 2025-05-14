@file:Suppress("ClassName", "ktlint:standard:filename")

package com.github.hubvd.odootools.libgit.legacy

import java.lang.foreign.MemoryLayout
import java.lang.foreign.StructLayout
import java.lang.foreign.ValueLayout

object git_status_options {
    val layout: StructLayout = MemoryLayout.structLayout(
        ValueLayout.JAVA_INT.withName("version"),
        ValueLayout.JAVA_INT.withName("show"),
        ValueLayout.JAVA_INT.withName("flags"),
        MemoryLayout.paddingLayout(4),
        git_strarray.layout.withName("pathspec"),
        ValueLayout.ADDRESS.withName("baseline"),
        ValueLayout.JAVA_SHORT.withName("rename_threshold"),
        MemoryLayout.paddingLayout(6),
    )!!
}
