package com.github.hubvd.odootools.odoo

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.odoo.commands.CompleteCommand
import com.github.hubvd.odootools.odoo.commands.LaunchCommand
import com.github.hubvd.odootools.workspace.WORKSPACE_MODULE
import com.github.hubvd.odootools.workspace.WorkspaceProvider
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import java.net.URLEncoder
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.system.exitProcess

fun LaunchCommand.computes() {
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
    option("http-port") { "0" }

    depends("test-tags", "test-qunit") {
        flag("test-enable") { testTags != null || testQunit != null }
    }

    depends("test-qunit", "test-tags", "addons-path") {
        option("init") {
            if (testQunit != null) return@option "qunit"
            val testTags = testTags ?: return@option null
            testTags
                .splitToSequence(',')
                .filter { !it.startsWith('-') }
                .flatMap { TestTag(it).toAddons(workspace, addonsPath) }
                .toHashSet()
                .joinToString(",")
        }
    }

    depends("community") {
        option("addons-path") {
            buildList {
                add("odoo/addons")
                if (!community && (workspace.path / "enterprise").isDirectory()) {
                    add("enterprise")
                }
                add("~/odoo-tools/addons")
            }.joinToString(",")
        }
    }

    env("QUNIT_FILTER") {
        val testTags = testQunit ?: return@env null
        URLEncoder.encode(testTags, "utf-8")
    }

    env("QUNIT_MOBILE") {
        if (mobile) "1" else null
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
            when {
                testQunit != null && mobile -> "/qunit:WebSuiteMobile"
                testQunit != null -> "/qunit:WebSuite"
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

fun main(args: Array<String>) {
    val di = DI {
        bind { singleton { LaunchCommand(instance(), instance()).subcommands(CompleteCommand(instance())) } }
        bind { singleton { Terminal() } }
        import(WORKSPACE_MODULE)
        bind { singleton { WorkspaceProvider(instance()).cached() } }
    }

    val command by di.instance<LaunchCommand>()
    command.main(args)
}
