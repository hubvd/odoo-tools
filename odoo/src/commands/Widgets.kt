package com.github.hubvd.odootools.odoo.commands

import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.verticalLayout
import com.github.ajalt.mordant.widgets.Panel
import com.github.ajalt.mordant.widgets.Text
import com.github.hubvd.odootools.odoo.RunConfiguration

fun runConfigurationWidget(runConfiguration: RunConfiguration) = verticalLayout {
    cell(
        Panel(
            envText(runConfiguration),
            title = Text("Env"),
            titleAlign = TextAlign.LEFT,
            expand = true,
        ),
    )
    cell(
        Panel(
            argText(runConfiguration),
            title = Text("Arguments"),
            titleAlign = TextAlign.LEFT,
            expand = true,
        ),
    )
}

fun envText(runConfiguration: RunConfiguration): Text = Text(
    buildString {
        val padding = runConfiguration.env.keys.maxOf { it.length } + 2
        runConfiguration.env.forEach { (k, v) ->
            append(TextColors.green(k))
            repeat(padding - k.length) {
                append(' ')
            }
            append(TextColors.gray("="))
            append(' ')
            if (k == "PATH") {
                append(v.substringBefore(':'))
                append(":...")
            } else {
                append(v)
            }
            append('\n')
        }
        setLength(length - 1)
    },
)

fun argText(runConfiguration: RunConfiguration): Text = Text(
    buildString {
        val maxEqIndex = runConfiguration.args
            .maxOfOrNull { it.indexOf('=').takeIf { it != -1 } ?: it.length }
            ?: 0

        runConfiguration.args.forEach { arg ->
            val parts = arg.split('=', limit = 2)
            repeat(maxEqIndex - parts[0].length) {
                append(' ')
            }
            append(TextColors.magenta(parts[0]))
            if (parts.size > 1) {
                append(TextColors.gray("="))
                append(parts[1])
            }
            append('\n')
        }
        setLength(length - 1)
    },
)
