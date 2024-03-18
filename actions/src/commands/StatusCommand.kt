package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.rendering.TextAlign.LEFT
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.table.Borders.ALL
import com.github.ajalt.mordant.table.table
import com.github.hubvd.odootools.actions.git.Repository
import com.github.hubvd.odootools.workspace.Workspaces
import java.nio.file.Path
import kotlin.io.path.div

class StatusCommand(private val workspaces: Workspaces) : CliktCommand() {
    override fun run() {
        terminal.println(
            table {
                header {
                    style = brightRed + bold
                    row("", "odoo", "enterprise")
                }
                body {
                    column(0) {
                        align = LEFT
                        cellBorders = ALL
                        style = brightBlue
                    }

                    val res = workspaces.list().parallelStream().map {
                        Triple(it, line(it.path / "odoo"), line(it.path / "enterprise"))
                    }.toList()

                    for ((workspace, o, e) in res) {
                        row {
                            cell(workspace.name)
                            cell(o)
                            cell(e)
                        }
                    }
                }
            },
        )
    }

    private fun line(path: Path) = buildString {
        Repository.open(path).use {
            val head = it.head()
            if (head.isBranch()) {
                append(head.branchName())
            } else {
                append(it.shortId(head))
            }
            val upstream = head.upstream()
            if (upstream != null) {
                val (ahead, behind) = head.target()!!.aheadBehind(upstream.target()!!)
                if (behind != 0L) {
                    append(" ↓")
                    append(behind)
                }
                if (ahead != 0L) {
                    append(" ↑")
                    append(ahead)
                }
            } else {
                append(" ??")
            }
            if (it.status().count() > 0) {
                append(red(" •"))
            }
        }
    }
}
