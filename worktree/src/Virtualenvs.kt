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
                workspace.version < 16f -> "3.9"
                workspace.version < 17f -> "3.11"
                else -> "3.12"
            }
            val pythonPath = pythonProvider.installOrGetVersion(pythonVersion)
            cd(rootPath)
            run("virtualenv", workspace.base, "--python=$pythonPath", description = "Creating virtualenv")
            val pip = rootPath / workspace.base / "bin/pip"
            run("$pip", "install", "--upgrade", "pip", description = "Upgrading pip")
            run(
                "$pip",
                "install",
                "-r",
                "${generateRequirements(workspace.path / "odoo/requirements.txt", workspace.version)}",
                description = "Installing packages",
            )
        }
        step("Linking virtualenv") {
            val target = workspace.path / "venv"
            target.deleteIfExists()
            target.createSymbolicLinkPointingTo(venvPath)
        }
    }

    private fun generateRequirements(requirements: Path, odooVersion: Float): Path {
        val output = Files.createTempFile("requirements-", ".txt")
        output.bufferedWriter().use { writer ->
            requirements.bufferedReader().use { it.copyTo(writer) }
            writer.appendLine()
            arrayOf(
                "pydevd-pycharm",
                "websocket-client",
                "mock",
                "pydevd-odoo",
                "rich",
                "ptpython",
                "dbfread",
            ).forEach { writer.appendLine(it) }
            if (14f < odooVersion && odooVersion < 16.4f) {
                writer.appendLine("rjsmin")
            }
        }
        output.toFile().deleteOnExit()
        return output
    }
}
