package com.github.hubvd.odootools.actions.commands.db

import com.github.ajalt.clikt.completion.CompletionCandidates

fun CompletionCandidates.Custom.Companion.databases(includeSavepoints: Boolean) = fromStdout(
    buildString {
        append("actions db list")
        if (includeSavepoints) {
            append(" --savepoints")
        }
    },
)
