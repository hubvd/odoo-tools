package com.github.hubvd.odootools.actions.utils

object Clipboard {
    private val isWayland = System.getenv("XDG_SESSION_TYPE") == "wayland"

    fun read(selection: Boolean = false): String {
        if (!isWayland) TODO("implement clipboard for x11")
        val process = ProcessBuilder()
            .command(
                buildList {
                    add("wl-paste")
                    add("--no-newline")
                    if (selection) {
                        add("--primary")
                    }
                },
            )
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()

        val content = process.inputStream.bufferedReader().use { it.readText() }
        if (process.waitFor() == 0) {
            return content
        } else {
            return ""
        }
    }
}
