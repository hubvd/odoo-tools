package com.github.hubvd.odootools.worktree.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.hubvd.odootools.workspace.Workspaces
import com.github.hubvd.odootools.worktree.OdooStubs
import com.github.hubvd.odootools.worktree.Pycharm
import com.github.hubvd.odootools.worktree.Virtualenvs
import com.github.hubvd.odootools.worktree.processSequence

class RebuildCommand(
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
                Pycharm(workspace).saveFiles()
            }
        }
    }
}
