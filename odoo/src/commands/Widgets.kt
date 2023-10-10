package com.github.hubvd.odootools.odoo.commands

import com.github.ajalt.mordant.rendering.*
import com.github.ajalt.mordant.table.verticalLayout
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Panel
import com.github.ajalt.mordant.widgets.Text
import com.github.hubvd.odootools.odoo.RunConfiguration

fun runConfigurationWidget(runConfiguration: RunConfiguration) = verticalLayout {
    cell(
        Panel(
            EnvWidget(runConfiguration),
            title = Text("Env"),
            titleAlign = TextAlign.LEFT,
            expand = true,
        ),
    )
    cell(
        Panel(
            ArgWidget(runConfiguration),
            title = Text("Arguments"),
            titleAlign = TextAlign.LEFT,
            expand = true,
        ),
    )
}

class EnvWidget(private val runConfiguration: RunConfiguration) : Widget {
    override fun measure(t: Terminal, width: Int): WidthRange = WidthRange(
        "workspace=${runConfiguration.context.workspace.path}".length,
        "workspace=${runConfiguration.context.workspace.path}".length,
    )

    override fun render(t: Terminal, width: Int) = Lines(
        buildList(2 + runConfiguration.env.size) {
            add(
                Line(
                    listOf(
                        Span.word("workspace", TextColors.green),
                        Span.word("=", TextColors.gray),
                        Span.word("${runConfiguration.context.workspace.path}"),
                    ),
                ),
            )
            add(
                Line(
                    listOf(
                        Span.word("version", TextColors.green),
                        Span.word("=", TextColors.gray),
                        Span.word("${runConfiguration.context.workspace.version}"),
                    ),
                ),
            )
            runConfiguration.env.forEach { (k, v) ->
                add(
                    Line(
                        listOf(
                            Span.word(k, TextColors.green),
                            Span.word("=", TextColors.gray),
                            Span.word(v),
                        ),
                    ),
                )
            }
        },
    )
}

class ArgWidget(private val runConfiguration: RunConfiguration) : Widget {
    override fun measure(t: Terminal, width: Int): WidthRange {
        val width = runConfiguration.args.maxOf { it.length }
        return WidthRange(width, width)
    }

    override fun render(t: Terminal, width: Int) = Lines(
        buildList(runConfiguration.args.size) {
            runConfiguration.args.forEach { arg ->
                val line = ArrayList<Span>(3)
                val parts = arg.split('=', limit = 2)
                line += Span.word(parts[0], style = TextColors.magenta)
                if (parts.size > 1) {
                    line += Span.word("=", style = TextColors.gray)
                    line += Span.word(parts[1])
                }
                add(Line(line))
            }
        },
    )
}
