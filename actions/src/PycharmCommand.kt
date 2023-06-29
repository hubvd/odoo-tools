package com.github.hubvd.odootools.actions

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.hubvd.odootools.workspace.Workspaces

class PycharmCommand(private val workspaces: Workspaces) : CliktCommand() {
    private val paths by argument().multiple()

    private val pathRe = "^(?<path>.*?)(?:#(?<line>\\d+)(?::(?<column>\\d+))?)?\$".toRegex()

    private fun extractPosition(pathDescriptor: String): Triple<String, Int?, Int?> {
        val match = pathRe.find(pathDescriptor) ?: return Triple(pathDescriptor, null, null)
        return Triple(
            match.groups["path"]?.value ?: "",
            match.groups["line"]?.value?.toInt(),
            match.groups["column"]?.value?.toInt(),
        )
    }

    override fun run() {
        val pycharm = Pycharm()
        if (paths.isNotEmpty()) {
            paths.forEach {
                val (path, line, column) = extractPosition(it)
                pycharm.open(path, line, column, blocking = true)
            }
        } else {
            val workspace = workspaces.current()
                ?: menu(workspaces.list()) { it.name }
                ?: throw Abort()

            pycharm.open(workspace.path.toString())
        }
    }
}
