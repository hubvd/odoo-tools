package com.github.hubvd.odootools.actions.commands.repo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.requireObject

class FetchCommand : CliktCommand() {
    private val workspaceRepositories by requireObject<List<WorkspaceRepositories>>()

    override fun run() {
        val branches = workspaceRepositories.map { it.workspace.base }.toHashSet()
        val path = workspaceRepositories.first().workspace.path

        val failure = arrayOf("odoo", "enterprise", "design-themes")
            .map {
                ProcessBuilder("git", "-C", "$path/$it", "fetch", "origin", *branches.toTypedArray())
                    .inheritIO()
                    .start()
            }
            .map {
                it.waitFor()
            }.find { it != 0 }

        if (failure != null) {
            throw ProgramResult(failure)
        }
    }
}
