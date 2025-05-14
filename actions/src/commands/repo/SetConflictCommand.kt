package com.github.hubvd.odootools.actions.commands.repo

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.hubvd.odootools.actions.utils.currentRepository
import com.github.hubvd.odootools.libgit.legacy.git_repository_state_t
import com.github.hubvd.odootools.workspace.Workspaces
import kotlin.io.path.div
import kotlin.io.path.writeText

class SetConflictCommand(private val workspaces: Workspaces) : CliktCommand() {
    private val ref by argument()

    override fun run() {
        val repo = workspaces.current()?.currentRepository() ?: throw Abort()
        val state = repo.state()

        if (state != git_repository_state_t.GIT_REPOSITORY_STATE_NONE) {
            throw CliktError("Repo in $state state")
        }

        val rev = repo.revParse(ref) ?: throw CliktError("Unknown reference $ref")
        val hash = rev.oid().hash() ?: throw Abort()
        println("resolved ${this.ref} to $hash")

        val repoPath = repo.path() ?: throw Abort()
        (repoPath / "CHERRY_PICK_HEAD").writeText(hash)
    }
}
