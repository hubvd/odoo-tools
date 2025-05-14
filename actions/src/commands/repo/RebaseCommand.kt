package com.github.hubvd.odootools.actions.commands.repo

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.requireObject
import com.github.hubvd.odootools.actions.git.currentRepositoryPath
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.io.path.name

class RebaseCommand : CliktCommand() {
    private val workspaceRepositories by requireObject<List<WorkspaceRepositories>>()

    override fun run(): Unit = runBlocking {
        val repos = workspaceRepositories.single()
        val workspace = repos.workspace
        val path = workspace.currentRepositoryPath() ?: throw Abort()

        val branch = async(repos.dispatcher) {
            // TODO
            val repo = when (path.name) {
                "odoo" -> repos.odoo
                "enterprise" -> repos.enterprise
                "design-themes" -> repos.designThemes
                else -> throw IllegalStateException()
            }.await()

            repo.head().takeIf { it.isBranch() }?.branchName()
        }.await()

        if (branch == null) {
            throw Abort()
        }

        val result = ProcessBuilder("git", "-C", "$path", "rebase", "origin/${workspace.base}")
            .inheritIO()
            .start()
            .waitFor()

        if (result != 0) {
            throw ProgramResult(result)
        }
    }
}
