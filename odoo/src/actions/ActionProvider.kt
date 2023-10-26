package com.github.hubvd.odootools.odoo.actions

import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.odoo.RunConfiguration
import com.github.hubvd.odootools.odoo.commands.DslContext
import com.github.hubvd.odootools.odoo.commands.runConfigurationWidget

fun interface Action {
    fun run(configuration: RunConfiguration)
}

fun interface ActionProvider {
    operator fun invoke(context: DslContext, terminal: Terminal): Action?
}

class ActionProviderImpl : ActionProvider {
    override fun invoke(context: DslContext, terminal: Terminal) = when {
        context.dryRun -> Action { terminal.println(runConfigurationWidget(it)) }
        context.save != null -> SavePycharmConfiguration(terminal, context.save!!, context.workspace)
        else -> LaunchAction(terminal)
    }
}
