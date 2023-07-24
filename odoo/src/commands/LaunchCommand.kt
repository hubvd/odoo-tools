package com.github.hubvd.odootools.odoo.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.odoo.ContextGenerator
import com.github.hubvd.odootools.odoo.actions.LaunchAction
import com.github.hubvd.odootools.odoo.actions.SavePycharmConfiguration
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

@DslMarker
annotation class CmdComputeDsl

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
}

class LaunchCommand(private val workspaces: Workspaces, private val terminal: Terminal) : CliktCommand(
    treatUnknownOptionsAsArgs = true,
    invokeWithoutSubcommand = true,
    name = "odoo",
) {

    private val ignores = HashSet<String>()

    private fun <T : Option> T.custom(): T = apply { ignores += id }
    private val arguments by argument().multiple()

    private val graph = HashMap<String, List<String>>()
    private val computes = HashMap<String, (MutableDslContext) -> Unit>()

    private val effects = ArrayList<(MutableDslContext) -> Unit>()
    private val envs = HashMap<String, (MutableDslContext) -> String?>()

    init {
        val patchedLauncherOptionGroup = PatchedLauncherOptionGroup()
        registerOptionGroup(patchedLauncherOptionGroup)
        patchedLauncherOptionGroup.options.forEach { registerOption(it.custom()) }

        val odooOptionGroup = OdooOptionGroup()
        registerOptionGroup(odooOptionGroup)
        odooOptionGroup.options.forEach { registerOption(it) }
    }

    data class Depends(val depends: List<String>)

    @CmdComputeDsl
    fun effect(block: DslContext.() -> Unit) {
        effects += block
    }

    @CmdComputeDsl
    fun depends(vararg tmpl: String, block: Depends.() -> Unit) {
        block(Depends(tmpl.toList()))
    }

    @CmdComputeDsl
    fun flag(name: String, block: DslContext.() -> Boolean) {
        computes[name] = { if (block(it)) it.flags += name }
    }

    @CmdComputeDsl
    fun option(name: String, block: DslContext.() -> String?) {
        computes[name] = { cmd -> block(cmd)?.let { cmd.options[name] = it } }
    }

    @CmdComputeDsl
    fun Depends.flag(name: String, block: DslContext.() -> Boolean) {
        graph[name] = depends
        computes[name] = { if (block(it)) it.flags += name }
    }

    @CmdComputeDsl
    fun Depends.option(name: String, block: DslContext.() -> String?) {
        graph[name] = depends
        computes[name] = { cmd -> block(cmd)?.let { cmd.options[name] = it } }
    }

    @CmdComputeDsl
    fun env(name: String, block: DslContext.() -> String?) {
        envs[name] = block
    }

    override fun run() {
        if (currentContext.invokedSubcommand != null) return

        computes()
        val workspace = workspaces.current() ?: workspaces.default()
        val runConfiguration = ContextGenerator(
            registeredOptions(),
            arguments,
            workspace,
            computes,
            graph,
            envs,
            effects,
            ignores,
        ).generate()

        val context = runConfiguration.context
        terminal.println(runConfigurationWidget(runConfiguration))

        val action = when {
            context.dryRun -> null
            context.save != null -> SavePycharmConfiguration(terminal, context.save!!, workspace)
            else -> LaunchAction(terminal)
        }

        action?.run(runConfiguration)
    }
}
