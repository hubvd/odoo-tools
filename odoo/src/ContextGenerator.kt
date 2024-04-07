package com.github.hubvd.odootools.odoo

import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.hubvd.odootools.odoo.commands.*
import com.github.hubvd.odootools.workspace.Workspace
import com.github.hubvd.odootools.workspace.WorkspaceConfig
import java.lang.reflect.Proxy
import java.nio.file.Path
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.typeOf

private typealias StringOption = OptionWithValues<String?, String, String>
private typealias Flag = OptionWithValues<Boolean, Boolean, Boolean>

data class RunConfiguration(
    val args: List<String>,
    val env: Map<String, String>,
    val cwd: Path,
    val context: DslContext,
    val effects: List<(DslContext) -> Unit>,
)

@DslMarker
annotation class CmdComputeDsl

typealias Effect = (DslContext) -> Unit

class ContextGenerator(
    private val options: List<Option>,
    private val arguments: List<String>,
    private val workspace: Workspace,
    private val ignores: Set<String>,
    private val config: WorkspaceConfig,
) {

    private val graph = HashMap<String, List<String>>()
    private val computes = HashMap<String, (MutableDslContext) -> Unit>()

    private val effects = ArrayList<Effect>()
    private val envs = HashMap<String, (MutableDslContext) -> String?>()

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

    fun generate(generator: ContextGenerator.() -> Unit): RunConfiguration {
        generator()
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

        val camelCaseRe = Regex("([a-z])([A-Z])")

        val proxy = Proxy.newProxyInstance(
            this.javaClass.classLoader,
            arrayOf(OdooOptions::class.java),
        ) { _, method, _ ->
            val getter = method.declaringClass.kotlin.declaredMemberProperties
                .find { it.javaGetter == method }
                ?: error("Unsupported method ${method.name}")

            val optionName = camelCaseRe.replace(getter.name, "\$1-\$2").lowercase()

            when (getter.returnType) {
                typeOf<String?>() -> options[optionName]

                typeOf<Boolean>() -> flags.contains(optionName)

                else -> error("Unsupported property type")
            }
        } as OdooOptions

        if (proxy.restart) {
            options.keys.removeAll(ignoreIfRestart)
            flags.removeAll(ignoreIfRestart)
        }

        val context = MutableDslContext(
            workspace = workspace,
            odooOptions = proxy,
            flags = flags,
            options = options,
            env = env,
            config = config,
        )

        val queue = computes.keys.toHashSet()
        queue.removeAll(flags)
        queue.removeAll(options.keys)

        if (context.restart) {
            queue.removeAll(ignoreIfRestart)
        }

        val filteredEffects: List<Effect> = if (context.dryRun || context.restart) {
            emptyList()
        } else {
            effects
        }

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

        val args = buildList(arguments.size + context.flags.size + context.options.size) {
            val args = arguments.toMutableList()
            if (args.remove("shell")) add("shell")
            addAll(args)
            context.flags
                .filter { it !in ignores }
                .forEach {
                    if (it == "odoo-help") {
                        add("--help") // FIXME
                    } else {
                        add("--$it")
                    }
                }
            context.options
                .filter { it.key !in ignores }
                .forEach { add("--${it.key}=${it.value}") }
        }

        return RunConfiguration(args, context.env, workspace.path, context, filteredEffects)
    }
}
