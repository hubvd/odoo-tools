package com.github.hubvd.odootools.worktree

import com.github.ajalt.clikt.core.CliktError
import com.github.hubvd.odootools.workspace.Workspace
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

private fun repositories(root: Path) = sequenceOf("odoo", "enterprise", "odoo-stubs", "design-themes")
    .map { root / "master" / it }
    .filter { (it / ".git").exists() }
    .map { it.name }
    .toList()
    .also { if ("odoo" !in it) throw CliktError("Odoo repository not found in ${root / "master"}") }

context(ProcessSequenceDslContext)
suspend fun createGitWorktrees(
    root: Path,
    path: Path,
    base: String
) = repositories(root).forEach { repo ->
    run(
        "git",
        "-C",
        "${root / "master" / repo}",
        "fetch",
        "origin",
        base
    )

    run(
        "git",
        "-C",
        "${root / "master" / repo}",
        "worktree",
        "add",
        "-f",
        "${path / repo}",
        "origin/$base",
    )
}

context(ProcessSequenceDslContext)
suspend fun pruneGitWorktrees(root: Path) = repositories(root).forEach { repo ->
    run(
        "git",
        "-C",
        "${root / "master" / repo}",
        "worktree",
        "prune",
    )
}

private fun generateRequirements(requirements: Path): Path {
    val output = Files.createTempFile("requirements-", ".txt")
    output.bufferedWriter().use { writer ->
        requirements.bufferedReader().use { it.copyTo(writer) }
        arrayOf("pydevd-pycharm", "websocket-client", "mock", "pydevd-odoo", "rich").forEach {
            writer.newLine()
            writer.append(it)
        }
    }
    output.toFile().deleteOnExit()
    return output
}

context(ProcessSequenceDslContext)
suspend fun createVirtualenv(
    workspace: Workspace,
    pythonProvider: PythonProvider
) {
    val pythonVersion = when {
        workspace.version < 16 -> "3.9.15"
        else -> "3.10.8"
    }

    val pythonPath = pythonProvider.installOrGetVersion(pythonVersion)

    run("virtualenv", "venv", "--python=$pythonPath")
    val pip = workspace.path / "venv/bin/pip"
    run("$pip", "install", "--upgrade", "pip")
    run("$pip", "install", "-r", "${generateRequirements(workspace.path / "odoo/requirements.txt")}")
}
