package com.github.hubvd.odootools.odoo

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.hubvd.odootools.odoo.commands.CompletionType

fun odooCompletion(type: CompletionType): CompletionCandidates.Custom = CompletionCandidates.Custom {
    when (it) {
        CompletionCandidates.Custom.ShellType.FISH -> buildString {
            append("'(")
            append("commandline -cop | odoo complete \"--token=$(commandline -ot)\" --type=$type")
            append(")'")
        }

        else -> TODO()
    }
}
