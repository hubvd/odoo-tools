package com.github.hubvd.odootools.worktree.commands

import com.github.ajalt.clikt.completion.CompletionCandidates.Custom.Companion.fromStdout
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.workspace.Workspace
import com.github.hubvd.odootools.workspace.WorkspaceConfig
import com.github.hubvd.odootools.workspace.Workspaces
import com.github.hubvd.odootools.worktree.*
import kotlin.io.path.createDirectory
import kotlin.io.path.div
import kotlin.io.path.exists

class AddCommand(
    private val config: WorkspaceConfig,
    private val terminal: Terminal,
    private val workspaces: Workspaces,
    private val stubs: OdooStubs,
    private val venvs: Virtualenvs,
) : CliktCommand() {
    private val name by option().required()
        .check("name must be a valid path name") { "^[a-zA-Z0-9-_.]*$".toRegex().matches(it) }
    private val base by option(completionCandidates = fromStdout("worktree list base")).required()
    private val path by option().path(canBeFile = false).default(config.root)

    private val community by option().flag()

    override fun run() {
        if (workspaces.list().find { it.name == name } != null) throw CliktError("$name already exists")
        val path = path / name
        if (path.exists()) throw CliktError("$path already exists")
        path.createDirectory()
        val repositories = Repository.entries.filter { if (community) it != Repository.Enterprise else true }
        processSequence(terminal) {
            cd(path)
            createGitWorktrees(
                root = config.root,
                path = path,
                base = base,
                community = community,
            )
            val workspace = Workspace(name, path)
            venvs.create(workspace)
            stubs.create(workspace)
            Pycharm(workspace, repositories).saveFiles()
        }
        terminal.println((TextStyles.underline + TextStyles.bold + TextColors.green)("Worktree created"))
    }
}
