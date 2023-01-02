package com.github.hubvd.odootools.worktree.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.workspace.Workspaces
import com.github.hubvd.odootools.worktree.*
import kotlin.io.path.div
import kotlin.io.path.notExists

class RebuildCommand(
    private val terminal: Terminal,
    private val workspaces: Workspaces,
    private val stubs: OdooStubs,
    private val venvs: Virtualenvs,
) : CliktCommand() {

    private val all by option().flag()
    override fun run() {
        val selectedWorkspaces = if (all) {
            workspaces.list()
        } else {
            listOf(workspaces.current() ?: throw CliktError("Not inside a workspace"))
        }

        processSequence(terminal) {
            for (workspace in selectedWorkspaces) {
                venvs.create(workspace)
                stubs.create(workspace)
                val community = (workspace.path / "enterprise").notExists()
                val repositories = Repository.values().filter { if (community) it != Repository.Enterprise else true }
                Pycharm(workspace, repositories).saveFiles()
            }
        }
    }
}
