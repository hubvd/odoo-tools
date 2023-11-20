package com.github.hubvd.odootools.worktree.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.workspace.Workspaces
import com.github.hubvd.odootools.worktree.processSequence
import com.github.hubvd.odootools.worktree.pruneGitWorktrees

class PruneCommand(
    private val workspaces: Workspaces,
    private val terminal: Terminal,
) : CliktCommand() {
    override fun run() {
        processSequence(terminal) {
            pruneGitWorktrees(workspaces.default())
        }
    }
}
