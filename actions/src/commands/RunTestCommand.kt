package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.Abort
import com.github.hubvd.odootools.actions.utils.Sway
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI

class RunTestCommand(override val di: DI) : PycharmActionCommand() {
    override fun run() {
        if (selection.isEmpty()) throw Abort()

        val flags = when {
            file.endsWith(".js") -> listOf("--test-qunit", selection)
            file.endsWith(".py") -> listOf("--test-tags", ".$selection")
            else -> throw Abort()
        }

        val title = flags.last().take(20)

        runBlocking {
            Sway.runScratchpadIfClosed()
            process(
                "kitty",
                "@",
                "--to",
                "unix:/tmp/mykitty",
                "launch",
                "--hold",
                "--title",
                title,
                "--type",
                "tab",
                "--cwd",
                workspace.path.toString(),
                "odoo",
                *flags.toTypedArray(),
            )
            Sway.showScratchpad()
        }
    }
}
