package com.github.hubvd.odootools.worktree.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.workspace.Workspaces
import com.github.hubvd.odootools.worktree.Asdf
import com.github.hubvd.odootools.worktree.createVirtualenv
import com.github.hubvd.odootools.worktree.processSequence
import kotlin.io.path.div

class RebuildCommand(
    private val asdf: Asdf,
    private val terminal: Terminal,
    private val workspaces: Workspaces
) : CliktCommand() {
    override fun run() {
        val workspace = workspaces.current() ?: throw CliktError("Not inside a workspace")
        (workspace.path / "venv").toFile().deleteRecursively()
        processSequence(terminal) {
            createVirtualenv(workspace, asdf)
        }
        terminal.println((TextStyles.underline + TextStyles.bold + TextColors.green)("Virtualenv recreated"))
    }
}