package com.github.hubvd.odootools.actions.commands.db

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.hubvd.odootools.actions.utils.DbManager
import com.github.hubvd.odootools.actions.utils.Odooctl

class CopyCommand(private val dbManager: DbManager, private val odooctl: Odooctl) : CliktCommand() {
    private val from by argument(
        completionCandidates = CompletionCandidates.Custom.databases(includeSavepoints = true),
    )
    private val to by argument(
        completionCandidates = CompletionCandidates.Custom.databases(includeSavepoints = true),
    )

    override fun run() {
        odooctl.instances()
            .filter { it.database == from || it.database == to }
            .mapNotNull { odooctl.kill(it) }
            .forEach { it.get() }
        dbManager.copy(from, to)
    }
}
