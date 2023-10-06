package com.github.hubvd.odootools.odoo.commands

import com.github.ajalt.clikt.core.GroupableOption
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.output.Localization
import com.github.ajalt.clikt.output.ParameterFormatter
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.hubvd.odootools.odoo.odooCompletion

abstract class StoredOptionGroup(name: String) : OptionGroup(name) {
    val options = mutableListOf<GroupableOption>()

    override fun registerOption(option: GroupableOption) {
        super.registerOption(option)
        options += option
    }
}

class PatchedLauncherOptionGroup : StoredOptionGroup("Custom options") {
    private val noPatch by option("--no-patch", help = "Disable custom patches").flag()

    init {
        arrayOf(
            option("--mobile", help = "Launch the mobile QUnit suite").flag().checkPatched(),
            option("--watch", help = "Open a chrome tab for js tests").flag().checkPatched(),
            option("--step-delay", help = "Override the step delay for tours").checkPatched(),
            option("--debug", help = "Suspend the execution immediately by placing a breakpoint").flag().checkPatched(),
            option("--dry-run", help = "Print the generated config and exit").flag(),
            option("--drop", help = "Drop the database if it exists").flag(),
            option("--community", help = "Remove the enterprise repo from the addons path").flag(),
            option(
                "--test-qunit",
                help = "Launch a QUnit test/module",
                completionCandidates = odooCompletion(CompletionType.Qunit),
            ),
            option("--save", help = "Save the current arguments to a pycharm run configuration"),
        ).forEach { registerOption(it) }
    }

    @JvmName("checkPatchedFlag")
    private fun OptionWithValues<Boolean, Boolean, Boolean>.checkPatched() = validate {
        if (noPatch && it) throw PatchedArgumentError(this.option)
    }

    private fun RawOption.checkPatched() = validate {
        if (noPatch) throw PatchedArgumentError(this.option)
    }

    class PatchedArgumentError(private val option: Option) : UsageError(null) {
        override fun formatMessage(localization: Localization, formatter: ParameterFormatter) = buildString {
            append("option ")
            append(formatter.formatOption(option.names.maxBy { it.length }))
            append(" cannot be used with ")
            append(formatter.formatOption("--no-patch"))
        }
    }
}

class OdooOptionGroup : StoredOptionGroup("Odoo options") {
    init {
        arrayOf(
            option("--stop-after-init", help = "Stop the server after its initialization").flag(),
            option("--addons-path", help = "Specify additional addons paths"),
            option(
                "--log-handler",
                metavar = "PREFIX:LEVEL",
                help = """
                    Setup a handler at LEVEL for a given PREFIX${"\u0085"}
                    An empty PREFIX indicates the root logger${"\u0085"}
                    Example: "odoo.orm:DEBUG" or "werkzeug:CRITICAL" (default: ":INFO")
                """.trimIndent(),
            ),
            option("-p", "--http-port", help = "Listen port for the main HTTP service"),
            option("-d", "--database", help = "Specify the database name"),
            option("--odoo-help", help = "Show the output of odoo-bin --help").flag(),
            option("--test-tags", help = "Filter tests", completionCandidates = odooCompletion(CompletionType.TestTag)),
            option(
                "-i",
                "--init",
                help = "Install one or more modules",
                completionCandidates = odooCompletion(CompletionType.Addon),
            ),
            option(
                "-u",
                "--update",
                help = "Update one or more modules",
                completionCandidates = odooCompletion(CompletionType.Addon),
            ),
            option("--test-enable", help = "Enable unit tests").flag(),
            option("--test-file", help = "Launch a python test file")
        ).forEach { registerOption(it) }
    }
}
