package com.github.hubvd.odootools.actions

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.hubvd.odootools.workspace.Workspaces
import kotlin.system.exitProcess

class PycharmCommand(private val workspaces: Workspaces) : CliktCommand() {
    private val line by option("--line").int()
    private val column by option("--column").int()
    private val path by argument().optional()

    override fun run() {
        if (path != null) {
            Pycharm().open(path!!, line, column)
        } else {
            val workspace = workspaces.current()
                ?: menu(workspaces.list()) { it.name }
                ?: exitProcess(1)

            Pycharm().open(workspace.path.toString())
        }
    }
}
