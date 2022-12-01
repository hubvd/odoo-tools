package com.github.hubvd.odootools.worktree

import com.github.hubvd.odootools.workspace.Workspace
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

class Virtualenvs(private val pythonProvider: PythonProvider, dataDir: DataDir) {

    private val rootPath = dataDir["virtualenvs"].also { it.createDirectories() }

    context(ProcessSequenceDslContext)
    suspend fun create(workspace: Workspace) {
        val venvPath = rootPath / workspace.base
        if (venvPath.notExists()) {
            val pythonVersion = when {
                workspace.version < 16 -> "3.9.15"
                else -> "3.10.8"
            }
            val pythonPath = pythonProvider.installOrGetVersion(pythonVersion)
            cd(rootPath)
            run("virtualenv", workspace.base, "--python=$pythonPath")
            val pip = rootPath / workspace.base / "bin/pip"
            run("$pip", "install", "--upgrade", "pip")
            run("$pip", "install", "-r", "${generateRequirements(workspace.path / "odoo/requirements.txt")}")
        }
        val target = workspace.path / "venv"
        target.deleteIfExists()
        target.createSymbolicLinkPointingTo(venvPath)
    }

    private fun generateRequirements(requirements: Path): Path {
        val output = Files.createTempFile("requirements-", ".txt")
        output.bufferedWriter().use { writer ->
            requirements.bufferedReader().use { it.copyTo(writer) }
            arrayOf("pydevd-pycharm", "websocket-client", "mock", "pydevd-odoo", "rich", "ptpython").forEach {
                writer.newLine()
                writer.append(it)
            }
        }
        output.toFile().deleteOnExit()
        return output
    }

}