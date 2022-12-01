package com.github.hubvd.odootools.odoo

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.config.Config
import com.github.hubvd.odootools.odoo.commands.LaunchCommand
import com.github.hubvd.odootools.workspace.WorkspaceConfig
import com.github.hubvd.odootools.workspace.Workspaces
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
        if ("drop" in flags && "database" in options) {
            val count = runBlocking {
                process(
                    "psql",
                    "-Atc",
                    """
                    select count(*)
                    from pg_stat_activity
                    where backend_type = 'client backend'
                    and datname = '${options["database"]}'
                    """.trimIndent(),
                    "odoo",
                    stdout = Redirect.CAPTURE, stderr = Redirect.SILENT
                ).output.firstOrNull()?.trim()?.toIntOrNull() ?: 0
            }

            if (count == 0) {
                runBlocking {
                    process("dropdb", options["database"]!!, stdout = Redirect.SILENT, stderr = Redirect.SILENT)
                }
            } else {
                println("database is in use")
                exitProcess(1)
            }
        }
    }

    option("limit-time-cpu") { "99999" }
    option("limit-time-real") { "99999" }

    depends("test-tags", "test-qunit") {
        flag("test-enable") { "test-tags" in options || "test-qunit" in options }
    }

    depends("test-qunit", "test-tags", "addons-path") {
        option("init") {
            if ("test-qunit" in options) return@option "qunit"
            val testTags = options["test-tags"] ?: return@option null
            testTags
                .splitToSequence(',')
                .filter { !it.startsWith('-') }
                .flatMap { TestTag(it).toAddons(workspace, options["addons-path"]) }
                .joinToString(",")
        }
    }

    depends("community") {
        option("addons-path") {
            buildList {
                add("odoo/addons")
                if ("community" !in flags && (workspace.path / "enterprise").isDirectory()) {
                    add("enterprise")
                }
                add("~/odoo-tools/addons")
            }.joinToString(",")
        }
    }

    env("QUNIT_FILTER") {
        val testTags = options["test-qunit"] ?: return@env null
        URLEncoder.encode(testTags, "utf-8")
    }

    env("QUNIT_MOBILE") {
        if ("mobile" in flags) "1" else null
    }

    env("QUNIT_WATCH") {
        if ("watch" in flags) "1" else null
    }

    env("ODOO_WORKSPACE") {
        workspace.path.toString()
    }

    depends("test-qunit", "mobile") {
        option("test-tags") {
            when {
                "test-qunit" in options && "mobile" in flags -> "/qunit:WebSuiteMobile"
                "test-qunit" in options -> "/qunit:WebSuite"
                else -> null
            }
        }
    }

    depends("test-enable") {
        flag("stop-after-init") { "test-enable" in flags }
        option("log-level") { if ("test-enable" in flags) "test" else null }
        option("max-cron-thread") { if ("test-enable" in flags) "0" else null }
        option("http-port") { if ("test-enable" in flags) "9000" else null }
        option("database") { if ("test-enable" in flags) "${workspace.name}-test" else workspace.name }
    }

}

fun main(args: Array<String>) {
    val di = DI {
        bind { singleton { LaunchCommand(instance()) } }
        bind { singleton { Terminal(AnsiLevel.TRUECOLOR, interactive = true, hyperlinks = true) } }
        bind { singleton { Workspaces(instance()) } }
        bind { singleton { Config.get("workspace", WorkspaceConfig.serializer()) } }
    }

    val command by di.instance<LaunchCommand>()
    command.main(args)
}
