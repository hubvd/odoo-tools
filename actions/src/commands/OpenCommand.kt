package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.hubvd.odootools.actions.utils.BrowserService
import com.github.hubvd.odootools.actions.utils.Odooctl
import com.github.hubvd.odootools.actions.utils.menu

class OpenCommand(private val odooctl: Odooctl, private val browserService: BrowserService) : CliktCommand(
    help = "Open the selected odoo instance in a web browser",
) {
    private val qunit by option("-q", "--qunit", help = "If enabled, open the QUnit test page").flag()
    private val firefox by option("-f", "--firefox", help = "Use Firefox").flag()

    override fun run() {
        val instances = odooctl.instances()
        val choice = menu(instances) {
            buildString {
                append(it.workspace.name)
                if (it.database != it.workspace.name) {
                    append(" (")
                    append(it.database)
                    append(')')
                }
                append(" [")
                append(it.port)
                append(']')
            }
        } ?: return
        val path = if (qunit) "/web/tests" else "/web"
        val url = "${choice.baseUrl}$path?debug=assets"
        if (firefox) browserService.firefox(url) else browserService.chrome(url)
    }
}
