package com.github.hubvd.odootools.odoo.actions

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.odoo.RunConfiguration
import com.github.hubvd.odootools.workspace.Workspace
import org.redundent.kotlin.xml.Node
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml
import java.time.Instant
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.system.exitProcess

private fun Node.option(name: String, value: String) {
    "option" {
        attribute("name", name)
        attribute("value", value)
    }
}

class SavePycharmConfiguration(
    private val terminal: Terminal,
    private val name: String,
    private val workspace: Workspace,
) : Action {
    override fun run(configuration: RunConfiguration) {
        val conf = xml("component") {
            attribute("name", "ProjectRunConfigurationManager")
            "configuration" {
                attributes(
                    "default" to "false",
                    "name" to name,
                    "type" to "Odoo",
                    "factoryName" to "Odoo",
                    "nameIsGenerated" to "false",
                )
                "module" { attribute("name", workspace.name) }
                option("INTERPRETER_OPTIONS", "")
                option("PARENT_ENVS", "true")
                "envs" {
                    "env" {
                        attribute("name", "PYTHONUNBUFFERED")
                        attribute("value", "1")
                    }
                    configuration.env.forEach { (k, v) ->
                        "env" {
                            attribute("name", k)
                            attribute("value", v)
                        }
                    }
                }
                option("SDK_HOME", "")
                option("WORKING_DIRECTORY", "\$PROJECT_DIR\$")
                option("IS_MODULE_SDK", "true")
                option("ADD_CONTENT_ROOTS", "false")
                option("ADD_SOURCE_ROOTS", "false")
                "EXTENSION" {
                    attributes("ID" to "PythonCoverageRunConfigurationExtension", "runner" to "coverage.py")
                }
                option("SCRIPT_NAME", "\$PROJECT_DIR\$/odoo/odoo-bin")
                option("PARAMETERS", configuration.args.joinToString(" ").replace("~", "\$USER_HOME\$"))
                option("SHOW_COMMAND_LINE", "false")
                option("EMULATE_TERMINAL", "true")
                option("MODULE_MODE", "false")
                option("INPUT_FILE", "false")
                "method" { attribute("v", "2") }
            }
        }

        val fileName = "${Instant.now().epochSecond}.xml"
        val runConfigPath = workspace.path / ".idea/runConfigurations"
        runConfigPath.createDirectories()
        val path = runConfigPath / fileName
        if (path.exists()) {
            println("File $path already exists")
            exitProcess(1)
        }
        terminal.println("saved configuration to " + TextColors.brightCyan(path.toString()))
        path.writeText(conf.toString(PrintOptions(indent = "  ")))
    }
}
