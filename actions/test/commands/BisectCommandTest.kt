package com.github.hubvd.odootools.actions.commands

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.core.context
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.PrintRequest
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalInfo
import com.github.ajalt.mordant.terminal.TerminalInterface
import com.github.hubvd.odootools.actions.ACTION_MODULE
import com.github.hubvd.odootools.actions.utils.*
import com.github.hubvd.odootools.config.Config
import com.github.hubvd.odootools.workspace.FakeWorkspaces
import com.github.hubvd.odootools.workspace.StaticWorkspace
import com.github.hubvd.odootools.workspace.Workspaces
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.kodein.di.*
import java.nio.file.Path
import kotlin.io.path.div

class BisectCommandTest {
    companion object {

        @TempDir
        private lateinit var tempDir: Path
        private lateinit var workspaceRoot: Path

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            this.workspaceRoot = tempDir / "16.0"
        }
    }

    private val git = FakeGit(
        mapOf(
            workspaceRoot / "odoo" to """
                f6c2b5368e3abdc292e66dc04efc4031eb12f742 [FIX] mrp_account: AVCO product valuation with component cost 0
                37b5626f58284e13ecfe113842134fe5006f5517 [FIX] sale: fix decimal precision in the catalog
                6245f284337296d9b5b780f7f6e8e13f16b65260 [FIX] web: check if navLink exists
                0a16e7f7b49b94f5b74544dc96dfb7a057f587b5 [FIX] repair: clean default keys from context before creating stock.move
                c0fac4b7b8b66315a553337956999d8eb602ac13 [FIX] mrp: BOM control_panel layout
                092241c33980f9d15979c268ad7a7832bca700e9 [FIX] hr_expense: expense splitting redirection and display
            """.trimIndent(),
            workspaceRoot / "enterprise" to """
                b7a27ca150287ab66fb8fc0faa4da5b29b61508e [FIX] l10n_cl_edi: in debit notes wizard, exclude anglo saxon lines if they exist
                59ebb6c669c543bdf79cbee31f7d18d0cadb6410 [FIX] documents_project_sale: avoid generating workspaces when disabled
                404458a3e804ceaab5cba4c6fc54aa1db1675cda [FIX] l10n_cl_edi : fix creation of duplicate vendor bills
            """.trimIndent(),
        ),
    )

    private val runbot = object : Runbot {
        override fun batches(base: String) = listOf(
            ResolvedBatch("f6c2b5368e3abdc292e66dc04efc4031eb12f742", "b7a27ca150287ab66fb8fc0faa4da5b29b61508e"),
            ResolvedBatch("f6c2b5368e3abdc292e66dc04efc4031eb12f742", "59ebb6c669c543bdf79cbee31f7d18d0cadb6410"),
            ResolvedBatch("0a16e7f7b49b94f5b74544dc96dfb7a057f587b5", "59ebb6c669c543bdf79cbee31f7d18d0cadb6410"),
            ResolvedBatch("092241c33980f9d15979c268ad7a7832bca700e9", "404458a3e804ceaab5cba4c6fc54aa1db1675cda"),
            ResolvedBatch("092241c33980f9d15979c268ad7a7832bca700e9", "dfd6ffe5abd7dff4e2f432e0b17719967b9a1a36"),
        )
    }

    private val workspaces = FakeWorkspaces(
        listOf(
            StaticWorkspace(
                name = "16.0",
                path = workspaceRoot,
                version = 16.0f,
                base = "16.0",
            ),
        ),
        current = "16.0",
        default = "16.0",
    )

    private val di = DI {
        import(ACTION_MODULE, allowOverride = true)

        bind<Workspaces>(overrides = true) { instance(workspaces) }
        bind<Git>(overrides = true) { instance(git) }
        bind<Runbot>(overrides = true) { instance(runbot) }
        bind { singleton { new(::BisectCommand) } }
        bind<Config>(overrides = true) { singleton { throw IllegalStateException() } }
    }

    @Test
    fun test() {
        val command by di.instance<BisectCommand>()

        val printRequests = mutableListOf<PrintRequest>()
        var currentBatch: ResolvedBatch? = null

        val fakeTerminal = object : TerminalInterface {
            override val info: TerminalInfo = TerminalInfo(
                width = 79,
                height = 24,
                ansiLevel = AnsiLevel.NONE,
                ansiHyperLinks = false,
                outputInteractive = true,
                inputInteractive = true,
                crClearsLine = false,
            )

            override fun completePrintRequest(request: PrintRequest) {
                printRequests.add(request)
                if (request.text.startsWith("odoo ")) {
                    currentBatch = request.text.lines().take(2).let {
                        ResolvedBatch(
                            odoo = it[0].split(" ")[1],
                            enterprise = it[1].split(" ")[1],
                        )
                    }
                }
            }

            override fun readLineOrNull(hideInput: Boolean) = when (currentBatch) {
                ResolvedBatch(
                    "0a16e7f7b49b94f5b74544dc96dfb7a057f587b5",
                    "59ebb6c669c543bdf79cbee31f7d18d0cadb6410",
                ),
                -> "good"

                else -> "bad"
            }
        }

        command.context {
            terminal = Terminal(terminal.theme, terminal.tabWidth, fakeTerminal)
        }

        command.parse(emptyArray())

        assertThat(printRequests.last().text.trim()).isEqualTo(
            """
            Possibly bad odoo commits:
            f6c2b5368e3abdc292e66dc04efc4031eb12f742 [FIX] mrp_account: AVCO product valuation with component cost 0
            37b5626f58284e13ecfe113842134fe5006f5517 [FIX] sale: fix decimal precision in the catalog
            6245f284337296d9b5b780f7f6e8e13f16b65260 [FIX] web: check if navLink exists

            Possibly bad enterprise commits:
            59ebb6c669c543bdf79cbee31f7d18d0cadb6410 [FIX] documents_project_sale: avoid generating workspaces when disabled
            """.trimIndent(),
        )
    }
}
