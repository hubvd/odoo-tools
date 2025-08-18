package com.github.hubvd.odootools.odoo.commands

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.GroupableOption
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.output.Localization
import com.github.ajalt.clikt.output.ParameterFormatter
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.hubvd.odootools.odoo.odooCompletion
import kotlin.io.path.Path
import kotlin.io.path.isDirectory

abstract class StoredOptionGroup(name: String) : OptionGroup(name) {
    val options = mutableListOf<GroupableOption>()

    override fun registerOption(option: GroupableOption) {
        super.registerOption(option)
        options += option
    }
}

class CustomOptionGroup : StoredOptionGroup("Custom options") {
    init {
        arrayOf(
            option("--mobile", help = "Launch the mobile QUnit suite").flag(),
            option("--watch", help = "Open a chrome tab for js tests").flag(),
            option(
                "--step-delay",
                help = "Override the step delay for tours",
                completionCandidates = CompletionCandidates.Fixed(
                    hashSetOf(
                        "100",
                        "200",
                        "300",
                        "400",
                        "500",
                        "1000",
                    ),
                ),
            ),
            option("--debug", help = "Suspend the execution immediately by placing a breakpoint").flag(),
            option("--dry-run", help = "Print the generated config and exit").flag(),
            option("--drop", help = "Drop the database if it exists").flag(),
            option("--community", help = "Remove the enterprise repo from the addons path").flag(),
            option("--themes", help = "Add the design-themes repo to the addons path").flag(),
            option(
                "--test-qunit",
                help = "Launch a QUnit test/module",
                completionCandidates = odooCompletion(CompletionType.Qunit),
            ),
            option(
                "--test-hoot",
                help = "Launch a HOOT test/module",
                completionCandidates = odooCompletion(CompletionType.Hoot),
            ),
            option("--save", help = "Save the current arguments to a pycharm run configuration"),
            option("--restart").flag(),
            option(
                "--chrome",
                help = "Select a Chrome version",
                completionCandidates = CompletionCandidates.Custom.fromStdout(
                    "find /home/hubert/src/multichrome/ -maxdepth 1 -mindepth 1 -type d | xargs -n 1 basename",
                ),
            ).validate {
                val extraPath = Path("/home/hubert/src/multichrome/$it")
                require(extraPath.isDirectory()) { "$extraPath does not exist" }
            },
            option("--debug-chrome", help = "Add debug=True to start_tour+browser_js").flag(),
            option(
                "--chrome-break-on",
                help = "Defines pause on exceptions state.",
            ).choice("caught", "uncaught", "all"),
            option("--coverage").flag(),
            option("--coverage-data-file"),
            option("--debug-no-suspend").flag(),
            option("--patches").choice("none", "all", "rich", "tests", "progress", "minifier").split(","),
            option("-R", "--retries"),
        ).forEach { registerOption(it) }
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
            option(
                "-d",
                "--database",
                help = "Specify the database name",
                completionCandidates = CompletionCandidates.Custom.fromStdout("actions db list"),
            ),
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
            option("--test-file", help = "Launch a python test file", completionCandidates = CompletionCandidates.Path),
            option(
                "--load-language",
                help = "specifies the languages for the translations you want to be loaded",
                completionCandidates = odooCompletion(CompletionType.Lang),
            ),
        ).forEach { registerOption(it) }
    }
}
