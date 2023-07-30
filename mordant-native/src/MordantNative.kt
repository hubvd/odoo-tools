@file:Suppress("ClassName", "SpellCheckingInspection", "PropertyName", "unused")

package com.github.hubvd.odootools.mordant.native

import com.oracle.svm.core.annotate.Substitute
import com.oracle.svm.core.annotate.TargetClass
import com.oracle.svm.core.annotate.TargetElement
import org.graalvm.nativeimage.StackValue
import org.graalvm.nativeimage.c.CContext
import org.graalvm.nativeimage.c.function.CFunction
import org.graalvm.nativeimage.c.struct.CField
import org.graalvm.nativeimage.c.struct.CStruct
import org.graalvm.word.PointerBase

class Substitutions {

    @TargetClass(className = "com.github.ajalt.mordant.internal.LinuxMppImpls")
    class LinuxMppImpls {

        @Substitute
        @TargetElement(name = TargetElement.CONSTRUCTOR_NAME)
        fun init() {
        }

        @Substitute
        fun stdinInteractive() = PosixLibC.instance.isatty(STDIN_FILENO) == 1

        @Substitute
        fun stdoutInteractive() = PosixLibC.instance.isatty(STDOUT_FILENO) == 1

        @Substitute
        fun stderrInteractive() = PosixLibC.instance.isatty(STDERR_FILENO) == 1

        @Substitute
        fun getTerminalSize(): Pair<Int, Int>? {
            val size = StackValue.get(PosixLibC.WinSize::class.java)
            return if (PosixLibC.instance.ioctl(0, TIOCGWINSZ, size) < 0) {
                null
            } else {
                size.ws_col.toInt() to size.ws_row.toInt()
            }
        }
    }

    private companion object {
        const val STDIN_FILENO = 0
        const val STDOUT_FILENO = 1
        const val STDERR_FILENO = 2

        const val TIOCGWINSZ = 0x00005413
    }
}

@CContext(PosixLibC.Directives::class)
class PosixLibC {

    class Directives : CContext.Directives {
        override fun getHeaderFiles() = listOf("<unistd.h>", "<sys/ioctl.h>")
    }

    @CStruct("winsize", addStructKeyword = true)
    interface WinSize : PointerBase {

        @get:CField("ws_row")
        val ws_row: Short

        @get:CField("ws_col")
        val ws_col: Short

        @get:CField("ws_xpixel")
        val ws_xpixel: Short

        @get:CField("ws_ypixel")
        val ws_ypixel: Short
    }

    @CFunction("isatty")
    external fun isatty(fd: Int): Int

    @CFunction("ioctl")
    external fun ioctl(fd: Int, cmd: Int, winSize: WinSize?): Int

    companion object {
        val instance = PosixLibC()
    }
}
