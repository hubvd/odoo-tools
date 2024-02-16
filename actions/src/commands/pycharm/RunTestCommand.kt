package com.github.hubvd.odootools.actions.commands.pycharm

import com.github.ajalt.clikt.core.Abort
import com.github.hubvd.odootools.actions.utils.Kitty
import com.github.hubvd.odootools.workspace.Workspaces
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking

class RunTestCommand(override val workspaces: Workspaces) : BasePycharmAction() {
    override fun run() {
        if (selection.isEmpty()) throw Abort()

        val flags = when {
            file.endsWith(".js") -> listOf("--test-qunit", selection)
            file.endsWith(".py") -> listOf("--test-tags", ".$selection")
            else -> throw Abort()
        }

        val title = flags.last().take(20)

        runBlocking {
            Kitty.runIfClosed()
            Kitty.launch(
                "odoo",
                *flags.toTypedArray(),
                title = title,
                cwd = workspace.path.toString(),
                type = "tab",
            )
        }
    }
}
