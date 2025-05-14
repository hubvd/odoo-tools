@file:Suppress("ClassName", "ktlint:standard:filename")

package com.github.hubvd.odootools.libgit.legacy

import java.lang.foreign.MemoryLayout
import java.lang.foreign.StructLayout
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT

object git_checkout_options {
    val layout: StructLayout = MemoryLayout.structLayout(
        JAVA_INT.withName("version"),
        JAVA_INT.withName("checkout_strategy"),
        JAVA_INT.withName("disable_filters"),
        JAVA_INT.withName("dir_mode"),
        JAVA_INT.withName("file_mode"),
        JAVA_INT.withName("file_open_flags"),
        JAVA_INT.withName("notify_flags"),
        MemoryLayout.paddingLayout(4),
        ADDRESS.withName("notify_cb"),
        ADDRESS.withName("notify_payload"),
        ADDRESS.withName("progress_cb"),
        ADDRESS.withName("progress_payload"),
        git_strarray.layout.withName("paths"),
        ADDRESS.withName("baseline"),
        ADDRESS.withName("baseline_index"),
        ADDRESS.withName("target_directory"),
        ADDRESS.withName("ancestor_label"),
        ADDRESS.withName("our_label"),
        ADDRESS.withName("their_label"),
        ADDRESS.withName("perfdata_cb"),
        ADDRESS.withName("perfdata_payload"),
    ).withName("git_checkout_options")
}
