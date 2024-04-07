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
import java.nio.file.Path
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
    private val name by option().defaultLazy { base }
        .check("name must be a valid path name") { "^[a-zA-Z0-9-_.]*$".toRegex().matches(it) }
    private val base by option(completionCandidates = fromStdout("worktree list base")).required()
    private val path by option().path(canBeFile = false).default(config.root)

    override fun run() {
        if (workspaces.list().find { it.name == name } != null) throw CliktError("$name already exists")
        val path = path / name
        if (path.exists()) throw CliktError("$path already exists")
        path.createDirectory()
        val targetWorkspace = object : Workspace {
            override val name: String
                get() = this@AddCommand.name
            override val path: Path
                get() = path
            override val version: Float
                get() = throw UnsupportedOperationException()
            override val base: String
                get() = this@AddCommand.base
        }
        processSequence(terminal) {
            cd(path)
            createGitWorktrees(
                root = workspaces.default(),
                target = targetWorkspace,
                base = base,
                name = name,
            )
            val workspace = Workspace(name, path)
            venvs.create(workspace)
            stubs.create(workspace)
            Pycharm(workspace).saveFiles()
        }
        terminal.println((TextStyles.underline + TextStyles.bold + TextColors.green)("Worktree created"))
    }
}
