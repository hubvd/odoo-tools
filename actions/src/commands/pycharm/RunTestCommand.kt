package com.github.hubvd.odootools.actions.commands.pycharm

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.hubvd.odootools.actions.kitty.Kitty
import com.github.hubvd.odootools.workspace.Workspaces
import kotlin.io.path.Path
import kotlin.io.path.name

class RunTestCommand(override val workspaces: Workspaces, private val kitty: Kitty) : BasePycharmAction() {
    private val drop by option().flag()

    override fun run() {
        if (selection.isEmpty()) throw Abort()

        var test: String
        val flags = buildList {
            if (file.endsWith(".js")) {
                add("--test-qunit")
                test = selection
                add(test)
            } else if (file.endsWith(".py")) {
                add("--test-tags")
                val prefix = Path(file).parent?.takeIf { it.name == "tests" }?.let { "/${it.parent.name}" } ?: ""
                test = if (selection.startsWith("test_")) {
                    "$prefix.$selection"
                } else {
                    "$prefix:$selection"
                }
                add(test)
            } else {
                throw Abort()
            }

            if (drop) {
                add("--drop")
            }
        }

        kitty.use {
            val previousWindowId = ls().flatMap { it.tabs }
                .flatMap { it.windows }
                .find { it.userVars["test"] == workspace.name }
                ?.id

            val replace = previousWindowId != null
            if (replace) {
                focusWindow("id:$previousWindowId")
            }
            launch(
                "odoo",
                *flags.toTypedArray(),
                windowTitle = test,
                cwd = workspace.path.toString(),
                type = if (replace) "overlay-main" else "tab",
                hold = true,
                `var` = listOf("test=${workspace.name}"),
            )
            if (replace) {
                closeWindow("id:$previousWindowId")
            }
        }
    }
}
