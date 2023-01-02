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

context(Node)
private infix fun String.eq(value: String) {
    "option" {
        attribute("name", this@eq)
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
                    "type" to "PythonConfigurationType",
                    "factoryName" to "Python",
                )
                "module" { attribute("name", workspace.name) }
                "INTERPRETER_OPTIONS" eq ""
                "PARENT_ENVS" eq "true"
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
                "SDK_HOME" eq ""
                "WORKING_DIRECTORY" eq "\$PROJECT_DIR\$"
                "IS_MODULE_SDK" eq "true"
                "ADD_CONTENT_ROOTS" eq "false"
                "ADD_SOURCE_ROOTS" eq "false"
                "EXTENSION" {
                    attributes("ID" to "PythonCoverageRunConfigurationExtension", "runner" to "coverage.py")
                }
                "SCRIPT_NAME" eq "\$PROJECT_DIR\$/odoo/odoo-bin"
                "PARAMETERS" eq configuration.args.joinToString(" ").replace("~", "\$USER_HOME\$")
                "SHOW_COMMAND_LINE" eq "false"
                "EMULATE_TERMINAL" eq "true"
                "MODULE_MODE" eq "false"
                "INPUT_FILE" eq "false"
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
