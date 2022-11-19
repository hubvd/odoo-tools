package com.github.hubvd.odootools.worktree.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.workspace.WorkspaceFormat
import com.github.hubvd.odootools.workspace.Workspaces
import com.github.hubvd.odootools.workspace.format

class DefaultCommand(private val terminal: Terminal, private val workspaces: Workspaces) : CliktCommand() {
    private val format by argument().enum<WorkspaceFormat>().default(WorkspaceFormat.Json)

    override fun run() {
        val workspace = workspaces.default()
        terminal.println(workspace.format(format))
    }
}
