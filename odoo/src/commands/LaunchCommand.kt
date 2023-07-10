package com.github.hubvd.odootools.odoo.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.markdown.Markdown
import com.github.ajalt.mordant.rendering.*
import com.github.ajalt.mordant.table.grid
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.odoo.ContextGenerator
import com.github.hubvd.odootools.odoo.RunConfiguration
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
    val quiet: Boolean
    val debug: Boolean
    val testEnable: Boolean
}

class LaunchCommand(private val workspaces: Workspaces, private val terminal: Terminal) : CliktCommand(
    treatUnknownOptionsAsArgs = true,
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
        registerOption(option("--stop-after-init").flag())
        registerOption(option("--community").flag().custom())
        registerOption(option("--no-patch").flag().custom())
        registerOption(option("--dry-run").flag().custom())
        registerOption(option("--test-qunit").custom())
        registerOption(option("--addons-path"))
        registerOption(option("--log-handler"))
        registerOption(option("--mobile").flag().custom())
        registerOption(option("--watch").flag().custom())
        registerOption(option("--step-delay").custom())
        registerOption(option("--drop").flag().custom())
        registerOption(option("-p", "--http-port"))
        registerOption(option("-d", "--database"))
        registerOption(option("-h", "--help").flag())
        registerOption(option("--test-tags"))
        registerOption(option("-i", "--init"))
        registerOption(option("--save").custom())
        registerOption(option("-q", "--quiet").flag().custom())
        registerOption(option("--debug").flag().custom())
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

        if (!context.quiet) {
            terminal.println(
                grid {
                    row {
                        cell(Markdown("# Odoo")) {
                            columnSpan = 2
                        }
                    }
                    row {
                        align = TextAlign.CENTER
                        cell(EnvWidget(runConfiguration))
                        cell(ArgWidget(runConfiguration))
                    }
                },
            )
        }

        val action = when {
            context.dryRun -> null
            context.save != null -> SavePycharmConfiguration(terminal, context.save!!, workspace)
            else -> LaunchAction(terminal)
        }

        action?.run(runConfiguration)
    }
}

class EnvWidget(private val runConfiguration: RunConfiguration) : Widget {
    override fun measure(t: Terminal, width: Int): WidthRange {
        return WidthRange(
            "workspace=${runConfiguration.context.workspace.path}".length,
            "workspace=${runConfiguration.context.workspace.path}".length,
        )
    }

    override fun render(t: Terminal, width: Int) = Lines(
        buildList(2 + runConfiguration.env.size) {
            add(
                Line(
                    listOf(
                        Span.word("workspace", TextColors.green),
                        Span.word("=", TextColors.gray),
                        Span.word("${runConfiguration.context.workspace.path}"),
                    ),
                ),
            )
            add(
                Line(
                    listOf(
                        Span.word("version", TextColors.green),
                        Span.word("=", TextColors.gray),
                        Span.word("${runConfiguration.context.workspace.version}"),
                    ),
                ),
            )
            runConfiguration.env.forEach { (k, v) ->
                add(
                    Line(
                        listOf(
                            Span.word(k, TextColors.green),
                            Span.word("=", TextColors.gray),
                            Span.word(v),
                        ),
                    ),
                )
            }
        },
    )
}

class ArgWidget(private val runConfiguration: RunConfiguration) : Widget {
    override fun measure(t: Terminal, width: Int): WidthRange {
        val width = runConfiguration.args.maxOf { it.length }
        return WidthRange(width, width)
    }

    override fun render(t: Terminal, width: Int) = Lines(
        buildList(runConfiguration.args.size) {
            runConfiguration.args.forEach { arg ->
                val line = ArrayList<Span>(3)
                val parts = arg.split('=', limit = 2)
                line += Span.word(parts[0], style = TextColors.magenta)
                if (parts.size > 1) {
                    line += Span.word("=", style = TextColors.gray)
                    line += Span.word(parts[1])
                }
                add(Line(line))
            }
        },
    )
}
