package com.github.hubvd.odootools.odoo

import com.github.ajalt.clikt.parameters.options.FlagOption
import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.hubvd.odootools.odoo.commands.DslContext
import com.github.hubvd.odootools.odoo.commands.id
import com.github.hubvd.odootools.workspace.Workspace
import java.nio.file.Path

private typealias StringOption = OptionWithValues<String?, String, String>
private typealias Flag = FlagOption<Boolean>

data class RunConfiguration(
    val args: List<String>,
    val env: Map<String, String>,
    val cwd: Path,
    val context: DslContext,
)

class ContextGenerator(
    private val options: List<Option>,
    private val arguments: List<String>,
    private val workspace: Workspace,
    private val computes: Map<String, (DslContext) -> Unit>,
    private val graph: Map<String, List<String>>,
    private val envs: Map<String, (DslContext) -> String?>,
    private val effects: List<(DslContext) -> Unit>,
    private val ignores: Set<String>,
) {
    fun generate(dryRun: Boolean): RunConfiguration {
        val flags: HashSet<String>
        val options: HashMap<String, String>

        this.options
            .partition { it is FlagOption<*> }
            .let {
                @Suppress("UNCHECKED_CAST")
                it as Pair<List<Flag>, List<StringOption>>
            }
            .let { (allFlags, allOptions) ->
                flags = allFlags.filter { it.value }
                    .map { it.id }
                    .toHashSet()
                options = allOptions.filter { it.value != null }
                    .associate { it.id to it.value!! }
                    .toMap(HashMap())
            }

        val context = DslContext(
            workspace = workspace,
            flags = flags,
            options = options,
            env = HashMap(),
        )

        val queue = computes.keys.toHashSet()
        queue.removeAll(context.flags)
        queue.removeAll(context.options.keys)

        // TODO: topological sort
        while (queue.isNotEmpty()) {
            val iter = queue.iterator()
            while (iter.hasNext()) {
                val compute = iter.next()
                val dependencies = graph[compute] ?: emptyList()
                if (dependencies.none { it in queue }) {
                    computes[compute]!!(context)
                    iter.remove()
                }
            }
        }

        envs.forEach { (name, func) -> func(context)?.let { context.env[name] = it } }

        if (!dryRun) {
            effects.forEach { it(context) }
        }

        val args = buildList(arguments.size + context.flags.size + context.options.size) {
            val args = arguments.toMutableList()
            if (args.remove("shell")) add("shell")
            addAll(args)
            context.flags
                .filter { it !in ignores }
                .forEach { add("--$it") }
            context.options
                .filter { it.key !in ignores }
                .forEach { add("--${it.key}=${it.value}") }
        }

        return RunConfiguration(args, context.env, workspace.path, context)
    }
}
