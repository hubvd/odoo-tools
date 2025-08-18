package com.github.hubvd.odootools.odoo.actions

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.HorizontalRule
import com.github.hubvd.odootools.odoo.RunConfiguration
import com.github.hubvd.odootools.odoo.commands.runConfigurationWidget
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

        terminal.rawPrint("\u001B]0;odoo:${configuration.odoo.database}\u0007")

        terminal.println(runConfigurationWidget(configuration))

        val useCustomLauncher = configuration.odoo.patches?.split(",")?.contains("none") != true

        val main = if (!useCustomLauncher) {
            "odoo/odoo-bin"
        } else {
            val launcherDir = unpackPatchedLauncher()
            (launcherDir / "main.py").toString()
        }

        val venv = Path(System.getenv("VIRTUAL_ENV") ?: "venv")
        val python = venv / "bin/python"

        val cmd = buildList {
            add(python.toString())
            if (configuration.odoo.coverage || configuration.odoo.coverageDataFile != null) {
                add("-m")
                add("coverage")
                add("run")
                configuration.odoo.coverageDataFile?.let {
                    add("--data-file")
                    add(configuration.odoo.coverageDataFile)
                }
            }
            add(main)
            addAll(configuration.args)
        }

        val retries = configuration.odoo.retries?.toInt() ?: 1
        var process: Process? = null

        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                process?.destroy()
                terminal.cursor.show()
                terminal.rawPrint("\u001B]9;4;0\u0007")
            },
        )

        repeat(retries) { retry ->
            if (retries != 1) {
                val percentage = (retry * 100) / retries
                terminal.rawPrint("\u001B]9;4;1;$percentage\u0007")
                terminal.println(HorizontalRule(TextColors.blue("[${retry + 1}/${retries}]")))
            }
            process = ProcessBuilder()
                .command(cmd)
                .inheritIO()
                .apply { environment().putAll(configuration.env) }
                .directory(configuration.odoo.workspace.path.toFile())
                .start()

            val code = process.waitFor()

            if (code != 0) {
                exitProcess(code)
            }
        }

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
