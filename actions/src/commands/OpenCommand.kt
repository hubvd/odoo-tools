package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.hubvd.odootools.actions.utils.BrowserService
import com.github.hubvd.odootools.actions.utils.OdooInstance
import com.github.hubvd.odootools.actions.utils.Odooctl
import com.github.hubvd.odootools.actions.utils.selectInstance
import kotlin.io.path.div
import kotlin.io.path.forEachLine

class OpenCommand(private val odooctl: Odooctl, private val browserService: BrowserService) : CliktCommand() {
    override fun help(context: Context) = "Open the selected odoo instance in a web browser"

    private val type by mutuallyExclusiveOptions(
        option("-q", "--qunit").flag().convert { Type.QUnit },
        option("-h", "--hoot", "-u", "--unit").flag().convert { Type.Hoot },
        name = "Test framework",
        help = "Choose the test framework to use",
    ).single()
    private val firefox by option("-f", "--firefox", help = "Use Firefox").flag()

    override fun run() {
        var instances = odooctl.instances()
        if (type == Type.Hoot) {
            instances = instances.filter { it.workspace.version >= 17.2f }
        }
        val choice = selectInstance(instances) ?: return
        val path = when (type) {
            Type.Hoot -> urls(choice).second
            Type.QUnit -> urls(choice).first
            else -> "/web"
        }
        val url = "${choice.baseUrl}$path?debug=assets"
        if (firefox) browserService.firefox(url) else browserService.chrome(url)
    }

    private fun urls(odoo: OdooInstance): Pair<String, String> {
        if (odoo.workspace.version < 17.2f) return "/web/tests" to "UNSUPPORTED"
        (odoo.workspace.path / "odoo/addons/web/controllers/webclient.py").forEachLine { line ->
            if (line.contains("/web/tests/legacy")) {
                return "/web/tests/legacy" to "/web/tests"
            } else if (line.contains("def test_suite(")) {
                return@forEachLine
            }
        }
        return "/web/tests" to "/web/tests/next"
    }

    private enum class Type { Hoot, QUnit }
}
