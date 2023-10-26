package com.github.hubvd.odootools.odoo.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.Option
import com.github.hubvd.odootools.odoo.ContextGenerator
import com.github.hubvd.odootools.odoo.actions.ActionProvider
import com.github.hubvd.odootools.odoo.computes
import com.github.hubvd.odootools.workspace.Workspace
import com.github.hubvd.odootools.workspace.Workspaces

interface DslContext : OdooOptions {
    val workspace: Workspace
}

class MutableDslContext(
    override val workspace: Workspace,
    private val odooOptions: OdooOptions,
    val flags: MutableSet<String>,
    val options: MutableMap<String, String>,
    val env: MutableMap<String, String>,
) : DslContext, OdooOptions by odooOptions

val Option.id: String get() = names.maxBy { it.length }.removePrefix("--")

interface OdooOptions {
    val community: Boolean
    val noPatch: Boolean
    val dryRun: Boolean
    val testQunit: String?
    val addonsPath: String?
    val mobile: Boolean
    val watch: Boolean
    val stepDelay: String?
    val drop: Boolean
    val database: String?
    val testTags: String?
    val save: String?
    val debug: Boolean
    val testEnable: Boolean
    val testFile: String?
}

class LaunchCommand(private val workspaces: Workspaces, private val actionProvider: ActionProvider) : CliktCommand(
    treatUnknownOptionsAsArgs = true,
    invokeWithoutSubcommand = true,
    name = "odoo",
) {

    private val ignores = HashSet<String>()

    private fun <T : Option> T.custom(): T = apply { ignores += id }
    private val arguments by argument().multiple()

    init {
        val patchedLauncherOptionGroup = PatchedLauncherOptionGroup()
        registerOptionGroup(patchedLauncherOptionGroup)
        patchedLauncherOptionGroup.options.forEach { registerOption(it.custom()) }

        val odooOptionGroup = OdooOptionGroup()
        registerOptionGroup(odooOptionGroup)
        odooOptionGroup.options.forEach { registerOption(it) }
    }

    override fun run() {
        if (currentContext.invokedSubcommand != null) return

        val workspace = workspaces.current() ?: workspaces.default()
        val runConfiguration = ContextGenerator(
            registeredOptions(),
            arguments,
            workspace,
            ignores,
        ).generate(computes)

        actionProvider(runConfiguration.context, currentContext.terminal)?.run(runConfiguration)
    }
}
