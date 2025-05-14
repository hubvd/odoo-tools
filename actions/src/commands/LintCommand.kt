package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.FileNotFound
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.notExists

class LintCommand : CliktCommand() {
    private val fix by option("-f", "--fix").flag()
    private val files by argument(completionCandidates = CompletionCandidates.Path).multiple(required = true)

    override fun run() {
        val home = Path(System.getProperty("user.home"))
        val root = home / "odoo-tools/lint"
        val eslintRc = root / ".eslintrc.json"
        val exe = root / "node_modules/.bin/eslint"
        if (exe.notExists()) {
            throw FileNotFound("$exe")
        }
        val args = buildList {
            add("--config=$eslintRc")
            if (fix) {
                add("--fix")
            }
            add("--resolve-plugins-relative-to=$root")
            add("--")
            files.forEach { file ->
                add(root.relativize(Path(file).toAbsolutePath()).toString())
            }
        }
        val exitCode = ProcessBuilder()
            .command(exe.toString(), *args.toTypedArray())
            .directory(root.toFile())
            .inheritIO()
            .start()
            .waitFor()

        if (exitCode != 0) {
            throw ProgramResult(exitCode)
        }
    }
}
