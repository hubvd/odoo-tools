package com.github.hubvd.odootools.odoo

import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.config.CONFIG_MODULE
import com.github.hubvd.odootools.odoo.actions.ActionProvider
import com.github.hubvd.odootools.odoo.actions.ActionProviderImpl
import com.github.hubvd.odootools.odoo.commands.CompleteCommand
import com.github.hubvd.odootools.odoo.commands.LaunchCommand
import com.github.hubvd.odootools.workspace.WORKSPACE_MODULE
import com.github.hubvd.odootools.workspace.WorkspaceProvider
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking
import org.kodein.di.*
import java.net.URLEncoder
import kotlin.io.path.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.system.exitProcess

val computes: ContextGenerator.() -> Unit = {
    effect {
        if (drop && database != null) {
            val count = runBlocking {
                process(
                    "psql",
                    "-Atc",
                    """
                    select count(*)
                    from pg_stat_activity
                    where backend_type = 'client backend'
                    and datname = '$database'
                    """.trimIndent(),
                    "odoo",
                    stdout = Redirect.CAPTURE,
                    stderr = Redirect.SILENT,
                ).output.firstOrNull()?.trim()?.toIntOrNull() ?: 0
            }

            if (count == 0) {
                runBlocking {
                    process("dropdb", database!!, stdout = Redirect.SILENT, stderr = Redirect.SILENT)
                }
            } else {
                println("database is in use")
                exitProcess(1)
            }
        }
    }

    option("limit-time-cpu") { "99999" }
    option("limit-time-real") { "99999" }

    depends("database") {
        option("http-port") {
            when {
                workspace.name == workspace.base && database == workspace.name -> {
                    (workspace.version * 100).roundToInt()
                }

                workspace.name == workspace.base && database == "${workspace.name}-test" -> {
                    (workspace.version * 100 + 5).roundToInt()
                }

                else -> {
                    val min = 2000
                    val max = 65535
                    (min + database.hashCode().absoluteValue % (max - min + 1))
                }
            }.let {
                when (it) {
                    1720 -> 1721 // reserved port..
                    else -> it
                }
            }.toString()
        }
    }

    depends("test-tags", "test-file") {
        flag("test-enable") { testTags != null || testFile != null }
    }

    depends("test-tags", "addons-path") {
        option("init") {
            val testTags = testTags ?: return@option null
            testTags
                .splitToSequence(',')
                .filter { !it.startsWith('-') }
                .flatMap { TestTag(it).toAddons(workspace, addonsPath) }
                .toHashSet()
                .joinToString(",")
                .takeUnless { it.isEmpty() }
        }
    }

    depends("community") {
        option("addons-path") {
            buildList {
                add("odoo/addons")
                if (!community) add("enterprise")
                if (themes) add("design-themes")
                val customAddonsPath = config.odooToolsPath / "addons"
                val home = Path(System.getProperty("user.home"))
                if (customAddonsPath.startsWith(home)) {
                    add("~/" + customAddonsPath.relativeTo(home))
                } else {
                    add(customAddonsPath)
                }
            }.joinToString(",")
        }
    }

    env("HOOT_FILTER") {
        val filter = testHoot ?: return@env null
        URLEncoder.encode(filter, "utf-8")
    }

    env("QUNIT_FILTER") {
        val testTags = testQunit ?: return@env null
        URLEncoder.encode(testTags, "utf-8")
    }

    env("QUNIT_WATCH") {
        if (watch) "1" else null
    }

    env("STEP_DELAY") {
        stepDelay?.toIntOrNull()?.toString()
    }

    env("ODOO_WORKSPACE") {
        workspace.path.toString()
    }

    env("ODOO_DEBUG") {
        if (debug) "1" else null
    }

    depends("test-qunit", "mobile") {
        option("test-tags") {
            if (testHoot != null && workspace.version < 17.2f) {
                throw UsageError("HOOT not available before 17.2")
            }
            when {
                testHoot != null && mobile -> "/qunit:WebSuiteMobile.test_hoot"
                testHoot != null -> "/qunit:WebSuite.test_hoot"
                testQunit != null && mobile -> "/qunit:WebSuiteMobile.test_qunit"
                testQunit != null -> "/qunit:WebSuite.test_qunit"
                else -> null
            }
        }
    }

    depends("test-enable") {
        flag("stop-after-init") { testEnable }
        option("log-level") { if (testEnable) "test" else null }
        option("max-cron-thread") { if (testEnable) "0" else null }
        option("database") { if (testEnable) "${workspace.name}-test" else workspace.name }
        option("log-handler") { if (testEnable) "werkzeug:ERROR" else null }
    }
}

internal val ODOO_MODULE by DI.Module {
    bind { singleton { new(::LaunchCommand).subcommands(new(::CompleteCommand)) } }
    bind { singleton { Terminal() } }
    import(WORKSPACE_MODULE)
    import(CONFIG_MODULE)
    bind { singleton { new(::WorkspaceProvider).cached() } }
    bind<ActionProvider> { singleton { ActionProviderImpl() } }
}

fun main(args: Array<String>) = DI { import(ODOO_MODULE) }.direct.instance<LaunchCommand>().main(args)
