package com.github.hubvd.odootools.odoo.actions

import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.odoo.Odoo
import com.github.hubvd.odootools.odoo.RunConfiguration
import com.github.hubvd.odootools.odoo.commands.runConfigurationWidget

fun interface Action {
    fun run(configuration: RunConfiguration)
}

fun interface ActionProvider {
    operator fun invoke(context: Odoo, terminal: Terminal): Action?
}

class ActionProviderImpl : ActionProvider {
    override fun invoke(odoo: Odoo, terminal: Terminal) = when {
        odoo.dryRun -> Action { terminal.println(runConfigurationWidget(it)) }
        odoo.save != null -> SavePycharmConfiguration(terminal, odoo.save!!, odoo.workspace)
        else -> LaunchAction(terminal)
    }
}
