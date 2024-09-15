package com.github.hubvd.odootools.worktree.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.hubvd.odootools.workspace.WorkspaceFormat
import com.github.hubvd.odootools.workspace.Workspaces
import com.github.hubvd.odootools.workspace.format

class ListCommand(private val workspaces: Workspaces) : CliktCommand() {
    private val format by argument().enum<WorkspaceFormat>().optional()
    override fun run() {
        val terminal = terminal
        val workspaces = workspaces.list()
        if (terminal.terminalInfo.outputInteractive && format == null) {
            terminal.println(
                table {
                    tableBorders = Borders.ALL
                    header {
                        style(TextColors.magenta, bold = true)
                        row("name", "path", "version", "base")
                    }
                    body {
                        rowStyles(TextColors.blue, TextColors.green)
                        cellBorders = Borders.LEFT_RIGHT
                        workspaces.forEach { w ->
                            row {
                                cell(w.name)
                                cell(TextStyles.hyperlink("file://${w.path}")(w.path.toString()))
                                cell(w.version)
                                cell(w.base)
                            }
                        }
                    }
                },
            )
        } else {
            workspaces.forEach {
                terminal.println(it.format(format ?: WorkspaceFormat.Json))
            }
        }
    }
}
