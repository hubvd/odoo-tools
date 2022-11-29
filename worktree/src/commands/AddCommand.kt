package com.github.hubvd.odootools.worktree.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.workspace.Workspace
import com.github.hubvd.odootools.workspace.WorkspaceConfig
import com.github.hubvd.odootools.workspace.Workspaces
import com.github.hubvd.odootools.worktree.PythonProvider
import com.github.hubvd.odootools.worktree.createGitWorktrees
import com.github.hubvd.odootools.worktree.createVirtualenv
import com.github.hubvd.odootools.worktree.processSequence
import kotlin.io.path.createDirectory
import kotlin.io.path.div
import kotlin.io.path.exists


class AddCommand(
    private val config: WorkspaceConfig,
    private val pythonProvider: PythonProvider,
    private val terminal: Terminal,
    private val workspaces: Workspaces,
) : CliktCommand() {
    private val name by option().required()
    private val base by option().required()

    override fun run() {
        if (workspaces.list().find { it.name == name } != null) throw CliktError("$name already exists")
        val path = config.root / name
        if (path.exists()) throw CliktError("$path already exists")
        path.createDirectory()
        processSequence(terminal) {
            cd(path)
            createGitWorktrees(
                root = config.root,
                path = path,
                base = base
            )
            createVirtualenv(Workspace(name, path), pythonProvider)
        }
        terminal.println((TextStyles.underline + TextStyles.bold + TextColors.green)("Worktree created"))
    }
}
