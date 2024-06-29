package com.github.hubvd.odootools.ffi.libffi.libgit

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

interface Struct : CModule {
    var arena: Arena
    val segment: MemorySegment?
}
