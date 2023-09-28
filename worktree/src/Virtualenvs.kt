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
                workspace.version < 16 -> "3.9.18"
                else -> "3.11.5"
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
                "${generateRequirements(workspace.path / "odoo/requirements.txt", workspace.version)}",
            )
        }
        val target = workspace.path / "venv"
        target.deleteIfExists()
        target.createSymbolicLinkPointingTo(venvPath)
    }

    private fun generateRequirements(requirements: Path, odooVersion: Float): Path {
        val output = Files.createTempFile("requirements-", ".txt")
        output.bufferedWriter().use { writer ->
            sequence {
                requirements.forEachLine { if (it.contains("==")) yield(it) }
                arrayOf(
                    "pydevd-pycharm",
                    "websocket-client",
                    "mock",
                    "pydevd-odoo",
                    "rich",
                    "ptpython",
                    "dbfread",
                ).forEach { yield(it) }
                if (14 < odooVersion && odooVersion < 16.4) {
                    yield("rjsmin")
                }
            }.forEach { writer.appendLine(it) }
        }
        output.toFile().deleteOnExit()
        return output
    }
}
