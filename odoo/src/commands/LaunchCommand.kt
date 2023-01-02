package com.github.hubvd.odootools.odoo.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.odoo.ContextGenerator
import com.github.hubvd.odootools.odoo.actions.LaunchAction
import com.github.hubvd.odootools.odoo.actions.SavePycharmConfiguration
import com.github.hubvd.odootools.odoo.computes
import com.github.hubvd.odootools.workspace.Workspace
import com.github.hubvd.odootools.workspace.Workspaces

data class DslContext(
    val workspace: Workspace,
    val flags: MutableSet<String>,
    val options: MutableMap<String, String>,
    val env: MutableMap<String, String>,
)

@DslMarker
annotation class CmdComputeDsl

val Option.id: String get() = names.maxBy { it.length }.removePrefix("--")

class LaunchCommand(private val workspaces: Workspaces, private val terminal: Terminal) : CliktCommand(treatUnknownOptionsAsArgs = true) {

    private val ignores = HashSet<String>()

    private fun <T : Option> T.custom(): T = apply { ignores += id }
    private val stopAfterInit by option("--stop-after-init").flag()
    private val community by option("--community").flag().custom()
    private val noPatch by option("--no-patch").flag().custom()
    private val dryRun by option("--dry-run").flag().custom()
    private val testQunit by option("--test-qunit").custom()
    private val addonsPath by option("--addons-path")
    private val mobile by option("--mobile").flag().custom()
    private val watch by option("--watch").flag().custom()
    private val drop by option("--drop").flag().custom()
    private val httpPort by option("-p", "--http-port")
    private val database by option("-d", "--database")
    private val help by option("-h", "--help").flag()
    private val arguments by argument().multiple()
    private val testTags by option("--test-tags")
    private val init by option("-i", "--init")
    private val save by option("--save").custom()

    private val graph = HashMap<String, List<String>>()
    private val computes = HashMap<String, (DslContext) -> Unit>()

    private val effects = ArrayList<(DslContext) -> Unit>()
    private val envs = HashMap<String, (DslContext) -> String?>()

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
        ).generate(dryRun)

        terminal.println(
            buildString {
                append(TextColors.magenta("workspace"))
                append('=')
                append(workspace.path.toString())
                append(' ')
                append(TextColors.magenta("version"))
                append('=')
                append(workspace.version.toString())
                appendLine()
                for (arg in runConfiguration.args) {
                    val parts = arg.split('=', limit = 2)
                    append(TextColors.magenta(parts[0].removePrefix("--")))
                    if (parts.size > 1) {
                        append('=')
                        append(parts[1])
                    }
                    append(' ')
                }
                if (runConfiguration.env.isNotEmpty()) appendLine()
                runConfiguration.env.forEach { (k, v) ->
                    append(TextColors.magenta(k))
                    append('=')
                    append(v)
                    append(' ')
                }
            },
        )

        val action = when {
            dryRun -> null
            save != null -> SavePycharmConfiguration(terminal, save!!, workspace)
            else -> LaunchAction()
        }

        action?.run(runConfiguration)
    }
}
