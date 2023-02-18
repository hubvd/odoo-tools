package com.github.hubvd.odootools.worktree

import com.github.hubvd.odootools.workspace.Workspace
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

class Virtualenvs(private val pythonProvider: PythonProvider, dataDir: DataDir) {

    private val rootPath = dataDir["virtualenvs"]

    context(ProcessSequenceDslContext)
    suspend fun create(workspace: Workspace) {
        rootPath.createDirectories()
        val venvPath = rootPath / workspace.base
        if (venvPath.notExists()) {
            val pythonVersion = when {
                workspace.version < 16 -> "3.9.15"
                else -> "3.11.2"
            }
            val pythonPath = pythonProvider.installOrGetVersion(pythonVersion)
            cd(rootPath)
            run("virtualenv", workspace.base, "--python=$pythonPath")
            val pip = rootPath / workspace.base / "bin/pip"
            run("$pip", "install", "--upgrade", "pip")
            run(
                "$pip",
                "install",
                "-r",
                "${generateRequirements(workspace.path / "odoo/requirements.txt", pythonVersion)}",
            )
        }
        val target = workspace.path / "venv"
        target.deleteIfExists()
        target.createSymbolicLinkPointingTo(venvPath)
    }

    private fun generateRequirements(requirements: Path, pythonVersion: String): Path {
        val allReplacements = mapOf(
            "3.11.2" to mapOf(
                "gevent" to "22.10.2",
                "greenlet" to "2.0.2",
                "lxml" to "4.9.2",
                "psycopg2" to "2.9.5",
                "reportlab" to "3.6.12",
            ),
        )

        val replacements = allReplacements[pythonVersion] ?: emptyMap()
        val output = Files.createTempFile("requirements-", ".txt")
        val matches = hashSetOf<String>()

        output.bufferedWriter().use { writer ->
            requirements.forEachLine { line ->
                val name = line.split("==", limit = 2).firstOrNull() ?: return@forEachLine
                if (name in replacements) {
                    matches += name
                } else {
                    writer.appendLine(line)
                }
            }
            matches.forEach { name ->
                writer.append(name)
                writer.append("==")
                writer.append(replacements[name]!!)
                writer.appendLine()
            }
            arrayOf(
                "pydevd-pycharm",
                "websocket-client",
                "mock",
                "pydevd-odoo",
                "rich",
                "ptpython",
                "dbfread",
            ).forEach {
                writer.newLine()
                writer.append(it)
            }
        }
        output.toFile().deleteOnExit()
        return output
    }
}
