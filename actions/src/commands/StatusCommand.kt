package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextAlign.LEFT
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.table.Borders.ALL
import com.github.ajalt.mordant.table.table
import com.github.hubvd.odootools.actions.git.Repository
import com.github.hubvd.odootools.actions.git.git_branch_t
import com.github.hubvd.odootools.workspace.Workspaces
import java.nio.file.Path
import kotlin.io.path.div

class StatusCommand(private val workspaces: Workspaces) : CliktCommand() {
    private val fast by option("-f", "--fast").flag()

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
                        Triple(
                            it,
                            line(it.path / "odoo", it.base),
                            line(it.path / "enterprise", it.base),
                        )
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

    private fun Pair<Long, Long>.formatAheadBehind(style: TextStyle, name: String? = null): String {
        return style(
            buildString {
                append('(')
                name?.let { append(name).append(": ") }
                second.let { append("↓").append(it) }
                first.let { append(" ↑").append(it) }
                append(')')
            },
        )
    }

    private fun line(path: Path, base: String) =
        Repository.open(path).use {
            buildList {
                val head = it.head()
                val headName = if (head.isBranch()) {
                    head.branchName()
                } else {
                    it.shortId(head)
                }
                add(headName!!)

                val upstream = head.upstream()?.target()
                if (upstream != null) {
                    add(head.target()!!.aheadBehind(upstream).formatAheadBehind(green))
                } else {
                    add(red("??"))
                }

                val baseUpstream = it.findBranch("origin/$base", git_branch_t.GIT_BRANCH_REMOTE)?.target()
                if (baseUpstream != null && (upstream == null || !upstream.isEqual(baseUpstream))) {
                    add(head.target()!!.aheadBehind(baseUpstream).formatAheadBehind(yellow, name = base))
                }

                if (!fast && it.status().count() > 0) {
                    add(red("•"))
                }
            }.joinToString(" ")
        }
}
