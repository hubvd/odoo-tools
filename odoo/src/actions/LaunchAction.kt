package com.github.hubvd.odootools.odoo.actions

import com.github.hubvd.odootools.odoo.RunConfiguration
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread
import kotlin.system.exitProcess

interface Action {
    fun run(configuration: RunConfiguration)
}

class LaunchAction : Action {
    override fun run(configuration: RunConfiguration) {
        val cmd = if ("no-patch" in configuration.context.flags || configuration.context.workspace.version < 14)
            listOf("venv/bin/python", "odoo/odoo-bin")
        else
            listOf("venv/bin/python", "/home/hubert/odoo-tools/patches/main.py") // FIXME: config ?

        val process =
            ProcessBuilder()
                .command(cmd + configuration.args)
                .inheritIO()
                .apply { environment().putAll(configuration.env) }
                .directory(configuration.context.workspace.path.toFile())
                .start()

        Runtime.getRuntime().addShutdownHook(thread(start = false) { process.destroy() })

        val code = process.waitFor()

        if ("test-enable" in configuration.context.flags) {
            runBlocking {
                process(
                    "notify-send", "-h",
                    *(if (code == 0) arrayOf("string:frcolor:#00FF00", "Tests passed")
                    else arrayOf("string:frcolor:#FF0000", "Tests failed"))
                )
            }
        }

        exitProcess(code)
    }
}
