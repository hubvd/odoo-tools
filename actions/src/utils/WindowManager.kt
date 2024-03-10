package com.github.hubvd.odootools.actions.utils

import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

interface WindowManager {
    fun raise(resourceClass: String)
}

class Kwin : WindowManager {
    override fun raise(resourceClass: String) {
        runKwinScript(
            """
            const client = workspace.clientList().find(e => e.resourceClass === "$resourceClass")
            if (client) workspace.activeClient = client
            """.trimIndent(),
        )
    }

    private fun runKwinScript(@Language("js") code: String, capture: Boolean = false): List<String> {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val temp = Files.createTempFile("kwin-script", ".js")
        val uuid = UUID.randomUUID()
        val codeWithPrint = """
            function print(message) {
                console.log(`$uuid:${'$'}{message}`);
            }
        """.trimIndent() + '\n' + code
        temp.writeText(codeWithPrint)

        val res: List<String>
        runBlocking {
            val id = process(
                "dbus-send",
                "--print-reply",
                "--dest=org.kde.KWin",
                "/Scripting",
                "org.kde.kwin.Scripting.loadScript",
                "string:$temp",
                stdout = Redirect.CAPTURE,
                stderr = Redirect.SILENT,
            ).output.last().substringAfterLast(" ").toInt()
            process(
                "dbus-send",
                "--print-reply",
                "--dest=org.kde.KWin",
                "/$id",
                "org.kde.kwin.Script.run",
                stdout = Redirect.SILENT,
                stderr = Redirect.SILENT,
            )
            process(
                "dbus-send",
                "--print-reply",
                "--dest=org.kde.KWin",
                "/$id",
                "org.kde.kwin.Script.stop",
                stdout = Redirect.SILENT,
                stderr = Redirect.SILENT,
            )
            process(
                "dbus-send",
                "--print-reply",
                "--dest=org.kde.KWin",
                "/Scripting",
                "org.kde.kwin.Scripting.unloadScript",
                "string:$temp",
                stdout = Redirect.SILENT,
                stderr = Redirect.SILENT,
            )
            if (capture) {
                res = process(
                    "journalctl",
                    "_COMM=kwin_wayland",
                    "-o",
                    "cat",
                    "--since",
                    now,
                    stdout = Redirect.CAPTURE,
                    stderr = Redirect.SILENT,
                )
                    .output
                    .filter { it.startsWith("js: $uuid:") }
                    .map { it.substring(41) }
            } else {
                res = emptyList()
            }
        }
        temp.deleteIfExists()
        return res
    }
}
