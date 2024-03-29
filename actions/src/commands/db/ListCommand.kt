package com.github.hubvd.odootools.actions.commands.db

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.hubvd.odootools.actions.utils.DbManager

class ListCommand(private val dbManager: DbManager) : CliktCommand() {
    private val savepoints by option("-s", "--savepoints").flag()

    override fun run() {
        dbManager.list(includeSavepoints = savepoints)?.forEach { println(it) } ?: throw Abort()
    }
}
