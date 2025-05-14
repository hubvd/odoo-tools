package com.github.hubvd.odootools.actions.commands.odooctl

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.hubvd.odootools.actions.utils.Odooctl
import com.github.hubvd.odootools.actions.utils.selectInstance

class KillCommand(private val odooctl: Odooctl) : CliktCommand() {
    private val all by option("-a", "--all").flag()
    private val database by argument().optional()

    override fun run() {
        val instances = odooctl.instances()
        if (all) {
            instances.forEach(odooctl::kill)
        } else if (database != null) {
            instances.find { it.database == database }?.let(odooctl::kill)
        } else {
            selectInstance(instances)?.let(odooctl::kill)
        }
    }
}
