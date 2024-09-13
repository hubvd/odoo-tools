package com.github.hubvd.odootools.odoo

import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.hubvd.odootools.odoo.commands.*
import com.github.hubvd.odootools.workspace.Workspace
import com.github.hubvd.odootools.workspace.WorkspaceConfig
import java.nio.file.Path

private typealias StringOption = OptionWithValues<String?, String, String>
private typealias Flag = OptionWithValues<Boolean, Boolean, Boolean>

data class RunConfiguration(
    val args: List<String>,
    val env: Map<String, String>,
    val cwd: Path,
    val odoo: Odoo,
    val effects: List<() -> Unit>,
)

class ContextGenerator(
    private val options: List<Option>,
    private val arguments: List<String>,
    private val workspace: Workspace,
    private val ignores: Set<String>,
    private val config: WorkspaceConfig,
) {

    fun generate(): RunConfiguration {
        val flags = HashSet<String>()
        val options = HashMap<String, String>()
        val env = HashMap<String, String>()

        val ignoreIfRestart = hashSetOf("init", "update")

        for (option in this.options) {
            when {
                option.nvalues == 0..0 -> {
                    @Suppress("UNCHECKED_CAST")
                    option as Flag
                    if (option.id != "help" && option.value) {
                        flags += option.id
                    }
                }

                else -> {
                    @Suppress("UNCHECKED_CAST")
                    option as StringOption
                    option.value?.let { options[option.id] = it }
                }
            }
        }

        val odoo = Odoo(workspace, config, options, flags)

        odoo.registeredFlags
            .filter { it.get() }
            .forEach { flags.add(it.name) }

        odoo.registeredOptions
            .mapNotNull { it.get()?.let { value -> it.name to value } }
            .forEach { options[it.first] = it.second }

        if (odoo.restart) {
            ignoreIfRestart.forEach { options.remove(it) }
        }

        odoo.registeredEnv
            .mapNotNull { it.get()?.let { value -> it.name to value } }
            .forEach { env[it.first] = it.second }

        val filteredEffects: List<() -> Unit> = if (odoo.dryRun || odoo.restart) {
            emptyList()
        } else {
            odoo.registeredEffects
        }

        val args = buildList(arguments.size + flags.size + options.size) {
            val args = arguments.toMutableList()
            if (args.remove("shell")) add("shell")
            addAll(args)
            flags
                .filter { it !in ignores }
                .forEach {
                    if (it == "odoo-help") {
                        add("--help") // FIXME
                    } else {
                        add("--$it")
                    }
                }
            options
                .filter { it.key !in ignores }
                .forEach { add("--${it.key}=${it.value}") }
        }

        return RunConfiguration(args, env, workspace.path, odoo, filteredEffects)
    }
}
