package com.github.hubvd.odootools.actions.commands.pycharm

import com.github.ajalt.clikt.core.Abort
import com.github.hubvd.odootools.actions.kitty.Kitty
import com.github.hubvd.odootools.workspace.Workspaces
import kotlinx.serialization.json.*
import kotlin.io.path.Path
import kotlin.io.path.name

class RunTestCommand(override val workspaces: Workspaces, private val kitty: Kitty) : BasePycharmAction() {
    override fun run() {
        if (selection.isEmpty()) throw Abort()

        val flags = when {
            file.endsWith(".js") -> listOf("--test-qunit", selection)
            file.endsWith(".py") -> {
                val prefix = Path(file).parent?.takeIf { it.name == "tests" }?.let { "/${it.parent.name}" } ?: ""
                if (selection.startsWith("test_")) {
                    listOf("--test-tags", "$prefix.$selection")
                } else {
                    listOf("--test-tags", "$prefix:$selection")
                }
            }

            else -> throw Abort()
        }

        val title = flags.last().take(20)
        kitty.use {
            val previousWindowId = ls().jsonArray
                .flatMap { it.jsonObject["tabs"]!!.jsonArray }
                .flatMap { it.jsonObject["windows"]!!.jsonArray.filterIsInstance<JsonObject>() }
                .find {
                    it["user_vars"]!!.jsonObject["test"]?.takeIf { it is JsonPrimitive }
                        ?.jsonPrimitive?.content == workspace.name
                }
                ?.get("id")?.jsonPrimitive?.int

            val replace = previousWindowId != null
            if (replace) {
                focusWindow("id:$previousWindowId")
            }
            launch(
                "odoo",
                *flags.toTypedArray(),
                windowTitle = title,
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
