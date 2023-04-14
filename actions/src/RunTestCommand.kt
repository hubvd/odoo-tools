package com.github.hubvd.odootools.actions

import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import kotlin.system.exitProcess

class RunTestCommand(override val di: DI) : PycharmActionCommand() {
    override fun run() {
        if (selection.isEmpty()) exitProcess(1)

        val flags = when {
            file.endsWith(".js") -> listOf("--test-qunit", selection)
            file.endsWith(".py") -> listOf("--test-tags", ".$selection")
            else -> exitProcess(1)
        }

        val title = flags.last().take(20)

        runBlocking {
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
        }
    }
}
