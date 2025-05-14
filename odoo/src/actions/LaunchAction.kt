package com.github.hubvd.odootools.odoo.actions

import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.odoo.RunConfiguration
import com.github.hubvd.odootools.odoo.commands.runConfigurationWidget
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Path
import java.util.zip.Adler32
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.outputStream
import kotlin.system.exitProcess

class LaunchAction(private val terminal: Terminal) : Action {

    override fun run(configuration: RunConfiguration) {
        configuration.effects.forEach { it() }

        if (System.getenv("TERM") == "xterm-kitty") {
            ProcessBuilder("kitty", "@", "set-window-title", "--temporary", "odoo:" + configuration.odoo.database)
                .apply {
                    redirectError(ProcessBuilder.Redirect.DISCARD)
                    redirectOutput(ProcessBuilder.Redirect.DISCARD)
                }
                .start()
                .waitFor()
        }

        terminal.println(runConfigurationWidget(configuration))

        val useCustomLauncher = !configuration.odoo.noPatch &&
            configuration.odoo.workspace.version > 14

        val main = if (!useCustomLauncher) {
            "odoo/odoo-bin"
        } else {
            val launcherDir = unpackPatchedLauncher()
            (launcherDir / "main.py").toString()
        }

        val venv = Path(System.getenv("VIRTUAL_ENV") ?: "venv")
        val python = venv / "bin/python"
        val cmd = listOf(python.toString(), main, *configuration.args.toTypedArray())

        val process =
            ProcessBuilder()
                .command(cmd)
                .inheritIO()
                .apply { environment().putAll(configuration.env) }
                .directory(configuration.odoo.workspace.path.toFile())
                .start()

        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                terminal.cursor.show()
                process.destroy()
            },
        )

        val code = process.waitFor()

        if (configuration.odoo.testEnable) {
            runBlocking {
                process(
                    "notify-send",
                    "-h",
                    *(
                        if (code == 0) {
                            arrayOf("string:frcolor:#00FF00", "Tests passed")
                        } else {
                            arrayOf("string:frcolor:#FF0000", "Tests failed")
                        }
                        ),
                )
            }
        }

        exitProcess(code)
    }

    private fun unpackPatchedLauncher(): Path {
        System.getenv("ODOO_TOOLS_LAUNCHER")?.run { return Path(this) }
        val dataDir = Path(System.getenv("XDG_DATA_HOME") ?: (System.getProperty("user.home") + "/.local/share"))
        val launcherDir = (dataDir / "odoo-tools/launcher").apply { createDirectories() }

        val expectedChecksums = parseChecksums()

        val actualChecksums: HashMap<Long, String> =
            launcherDir.toFile().walkTopDown().filter { it.isFile && it.name.endsWith(".py") }
                .associateTo(HashMap()) { file ->
                    file.checksum() to file.relativeTo(launcherDir.toFile()).toString()
                }

        if (expectedChecksums != actualChecksums) {
            launcherDir.toFile().deleteRecursively()
            launcherDir.createDirectories()

            val files = expectedChecksums.values
            files.map { (launcherDir / it).parent }.toHashSet().forEach { it.createDirectories() }

            files.forEach { res ->
                val stream = javaClass.getResourceAsStream("/launcher/$res")
                    ?: throw FileNotFoundException("resource `$res` not in classpath")

                stream.use { `in` ->
                    (launcherDir / res).outputStream().use { out ->
                        `in`.copyTo(out)
                    }
                }
            }
        }

        return launcherDir
    }

    private fun parseChecksums(): HashMap<Long, String> =
        (javaClass.getResource("/launcher/checksums") ?: error("missing checksums"))
            .readText()
            .lineSequence()
            .filter { it.isNotBlank() }
            .map { it.trim().split(':', limit = 2) }
            .associateTo(HashMap()) { it[0].toLong() to it[1] }

    private fun File.checksum(): Long {
        ADLER_32.reset()
        ADLER_32.update(readBytes())
        return ADLER_32.value
    }

    companion object {
        val ADLER_32 by lazy { Adler32() }
    }
}
