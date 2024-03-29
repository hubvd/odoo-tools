package com.github.hubvd.odootools.actions.commands.db

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.hubvd.odootools.actions.utils.DbManager

class DropCommand(private val dbManager: DbManager) : CliktCommand() {
    private val name by argument(
        completionCandidates = CompletionCandidates.Custom.databases(includeSavepoints = true),
    )

    override fun run() {
        if (!dbManager.delete(name)) {
            throw Abort()
        }
    }
}
