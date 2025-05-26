package com.github.hubvd.odootools.odoo

import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.terminal.Terminal
import com.github.hubvd.odootools.config.CONFIG_MODULE
import com.github.hubvd.odootools.odoo.actions.ActionProvider
import com.github.hubvd.odootools.odoo.actions.ActionProviderImpl
import com.github.hubvd.odootools.odoo.commands.CompleteCommand
import com.github.hubvd.odootools.odoo.commands.LaunchCommand
import com.github.hubvd.odootools.workspace.WORKSPACE_MODULE
import com.github.hubvd.odootools.workspace.Workspace
import com.github.hubvd.odootools.workspace.WorkspaceConfig
import com.github.hubvd.odootools.workspace.WorkspaceProvider
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking
import org.kodein.di.*
import kotlin.io.path.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.system.exitProcess

@Suppress("unused")
class Odoo(
    val workspace: Workspace,
    private val config: WorkspaceConfig,
    options: Map<String, String>,
    flags: Set<String>,
) : CliGenerator(options, flags) {
    val limitTimeCpu by option { "99999" }
    val limitTimeReal by option { "99999" }

    val testHoot by option()
    val testQunit by option()
    val mobile by flag()

    val testFile by option()

    val testTags by option {
        val hasHoot = workspace.version >= 17.2f

        if (testHoot != null && !hasHoot) {
            throw UsageError("HOOT not available before 17.2")
        }

        val (qunit, qunitMobile) = if (hasHoot) {
            "test_qunit_desktop" to "test_qunit_mobile"
        } else {
            "test_js" to "test_mobile_js"
        }

        when {
            testHoot != null && mobile -> "/web:MobileWebSuite.test_unit_mobile[$testHoot]"
            testHoot != null -> "/web:WebSuite.test_hoot$testHoot"
            testQunit != null && mobile -> "/web:MobileWebSuite.$qunitMobile[$testQunit]"
            testQunit != null -> "/web:WebSuite.$qunit[$testQunit]"
            else -> null
        }
    }

    val testEnable by flag { testTags != null || testFile != null }

    val httpPort by option {
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

    val community by flag()
    val themes by flag()
    val drop by flag()
    val watch by flag()
    val stepDelay by option()
    val debug by flag()
    val dryRun by flag()
    val restart by flag()
    val save by option()
    val noPatch by flag()
    val chrome by option()
    val debugChrome by flag()
    val coverage by flag()
    val coverageDataFile by option()
    val debugNoSuspend by flag()

    val database by option { if (testEnable) "${workspace.name}-test" else workspace.name }
    val logHandler by option { if (testEnable) "werkzeug:ERROR" else null }
    val logLevel by option { if (testEnable) "test" else null }
    val maxCronThread by option { if (testEnable) "0" else null }
    val stopAfterInit by flag { testEnable }

    val addonsPath by option {
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

    val init by option {
        val testTags = testTags ?: return@option null
        testTags.splitToSequence(',').filter { !it.startsWith('-') }
            .flatMap { TestTag(it).toAddons(workspace, addonsPath) }.toHashSet().joinToString(",")
            .takeUnless { it.isEmpty() }
    }

    val withoutDemo by option {
        if (workspace.version >= 18.3f && !testEnable) "False" else null
    }

    val odooWatchChrome by env { if (watch) "1" else null }
    val odooDebugChrome by env { if (debugChrome) "1" else null }
    val odooTourStepDelay by env { stepDelay?.toIntOrNull()?.toString() }
    val odooWorkspace by env { workspace.path.toString() }
    val odooDebug by env { if (debug) "1" else null }
    val path by env {
        if (chrome != null) {
            "/home/hubert/src/multichrome/$chrome:${System.getenv("PATH")}"
        } else {
            null
        }
    }
    val odooDebugSuspend by env { if (debugNoSuspend) "0" else null }

    init {
        effect {
            if (!drop || database == null) return@effect
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
