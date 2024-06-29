package com.github.hubvd.odootools.actions.commands.repo

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.hubvd.odootools.actions.git.Repository
import com.github.hubvd.odootools.actions.git.currentRepository
import com.github.hubvd.odootools.actions.git.git_repository_state_t
import com.github.hubvd.odootools.workspace.Workspaces
import java.lang.foreign.MemorySegment
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.writeText

class SetConflictCommand(private val workspaces: Workspaces) : CliktCommand() {
    private val ref by argument()

    override fun run() {
        val repo = workspaces.current()?.currentRepository() ?: throw Abort()
        val state = state(repo)

        if (state != git_repository_state_t.GIT_REPOSITORY_STATE_NONE) {
            throw CliktError("Repo in $state state")
        }

        val rev = repo.revParse(ref) ?: throw CliktError("Unknown reference $ref")
        val hash = rev.oid().hash() ?: throw Abort()
        println("resolved ${this.ref} to $hash")

        val repoPath = repo.proxy.repository_path(repo.address)
            .takeIf { it != MemorySegment.NULL }
            ?.reinterpret(Long.MAX_VALUE)
            ?.getString(0)
            ?.let { Path(it) }
            ?: throw Abort()

        (repoPath / "CHERRY_PICK_HEAD").writeText(hash)
    }

    private fun state(repo: Repository) =
        git_repository_state_t.entries.first { it.value == repo.proxy.repository_state(repo.address) }
}
