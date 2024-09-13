package com.github.hubvd.odootools.actions.commands.menu

import com.github.ajalt.clikt.core.CliktCommand
import com.github.hubvd.odootools.actions.utils.menu
import com.github.hubvd.odootools.pycharmctl.api.PycharmCtl
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.useDirectoryEntries

class AddonCommand(private val pycharm: PycharmCtl) : CliktCommand() {
    override fun run() {
        val project = pycharm.currentProject()
        val addons = buildList<Path>(1200) {
            for (addonPath in arrayOf(
                project.path / "odoo/addons",
                project.path / "odoo/odoo/addons",
                project.path / "enterprise",
            )) {
                addonPath.useDirectoryEntries {
                    it.forEach { entry ->
                        if ((entry / "__manifest__.py").exists()) {
                            add(entry)
                        }
                    }
                }
            }
        }
        val selectedAddon = menu(addons, lines = 20, prompt = "addon") { it.name } ?: return
        pycharm.openFile(selectedAddon)
        pycharm.focus(project)
    }
}
