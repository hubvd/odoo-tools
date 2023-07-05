package com.github.hubvd.odootools.actions

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class OpenCommand(private val odooctl: Odooctl, private val browserService: BrowserService) : CliktCommand() {
    private val qunit by option("-q").flag()
    private val firefox by option("-f").flag()

    override fun run() {
        val instances = odooctl.instances()
        val choice = menu(instances) { it.workspace.name } ?: return
        val path = if (qunit) "/web/tests" else "/web"
        val url = "${choice.baseUrl}$path?debug=assets"
        if (firefox) browserService.firefox(url) else browserService.chrome(url)
    }
}
