package com.github.hubvd.odootools.worktree

import com.github.hubvd.odootools.workspace.Workspace
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter
import kotlin.io.path.div

private val repositories = listOf("odoo", "enterprise", "odoo-stubs", "design-themes")

context(ProcessSequenceDslContext)
suspend fun createGitWorktrees(
    root: Path,
    path: Path,
    base: String
) = repositories.forEach { repo ->
    // TODO: fetch base ?
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
suspend fun pruneGitWorktrees(root: Path, ) = repositories.forEach { repo ->
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
    asdf: Asdf
) {
    val pythonVersion = when {
        workspace.version < 16 -> "3.9.15"
        else -> "3.10.8"
    }

    val pythonPath = asdf.run {
        if ("python" !in listPlugins()) addPlugin("python")
        if (pythonVersion !in listVersions("python")) addVersion("python", pythonVersion)
        where("python", pythonVersion) / "bin/python"
    }

    run("virtualenv", "venv", "--python=$pythonPath")
    val pip = workspace.path / "venv/bin/pip"
    run("$pip", "install", "--upgrade", "pip")
    run("$pip", "install", "-r", "${generateRequirements(workspace.path / "odoo/requirements.txt")}")
}
