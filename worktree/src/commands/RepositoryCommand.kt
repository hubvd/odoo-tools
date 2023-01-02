package com.github.hubvd.odootools.worktree.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.hubvd.odootools.workspace.Workspaces
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.system.exitProcess

class RepositoryCommand(private val workspaces: Workspaces) : CliktCommand() {
    private val path by option().flag()

    override fun run() {
        val workspace = workspaces.current() ?: exitProcess(1)
        val cwd = Path(System.getProperty("user.dir"))
        val repoPath = workspace.path.relativize(cwd).subpath(0, 1)
            .takeIf { it.toString().isNotEmpty() }
            ?.takeIf { (workspace.path / it / ".git").exists() }
            ?: exitProcess(1)

        if (path) {
            println(workspace.path / repoPath)
        } else {
            println(repoPath.name)
        }
    }
}
