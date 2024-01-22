package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.hubvd.odootools.actions.utils.BrowserService
import com.github.hubvd.odootools.actions.utils.Odooctl
import com.github.hubvd.odootools.actions.utils.selectInstance

class OpenCommand(private val odooctl: Odooctl, private val browserService: BrowserService) : CliktCommand(
    help = "Open the selected odoo instance in a web browser",
) {
    private val qunit by option("-q", "--qunit", help = "If enabled, open the QUnit test page").flag()
    private val firefox by option("-f", "--firefox", help = "Use Firefox").flag()

    override fun run() {
        val choice = selectInstance(odooctl.instances()) ?: return
        val path = if (qunit) "/web/tests" else "/web"
        val url = "${choice.baseUrl}$path?debug=assets"
        if (firefox) browserService.firefox(url) else browserService.chrome(url)
    }
}
