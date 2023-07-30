package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.actions.utils.Odooctl

class OdooctlCommand(private val odooctl: Odooctl, private val terminal: Terminal) : CliktCommand(
    help = "List information about running odoo instances",
) {
    private val killall by option("-k", "--kill-all").flag()

    override fun run() {
        if (killall) {
            odooctl.killAll()
            return
        }

        val instances = odooctl.instances().ifEmpty { return }
        terminal.println(
            table {
                tableBorders = Borders.ALL
                header {
                    style(TextColors.magenta, bold = true)
                    row("pid", "workspace", "db", "url")
                }
                body {
                    rowStyles(TextColors.blue, TextColors.green)
                    cellBorders = Borders.LEFT_RIGHT
                    instances.forEach { instance ->
                        row {
                            cell(instance.pid)
                            cell(instance.workspace.name)
                            cell(instance.database)
                            cell(instance.baseUrl)
                        }
                    }
                }
            },
        )
    }
}
