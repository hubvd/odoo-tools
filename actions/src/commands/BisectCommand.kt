package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.hubvd.odootools.actions.utils.*
import com.github.hubvd.odootools.workspace.Workspace
import com.github.hubvd.odootools.workspace.Workspaces
import kotlin.math.roundToInt

private data class BisectState(
    val low: Int,
    val mid: Int,
    val high: Int,
    val batch: ResolvedBatch,
    val batches: List<ResolvedBatch>,
    val workspace: Workspace,
) {
    companion object {
        operator fun invoke(low: Int, high: Int, batches: List<ResolvedBatch>, workspace: Workspace): BisectState {
            val mid = (low + high).ushr(1)
            return BisectState(low, mid, high, batches[mid], batches, workspace)
        }
    }
}

class BisectCommand(
    private val runbot: Runbot,
    private val workspaces: Workspaces,
    private val git: Git,
) : CliktCommand(
    help = "Bisect odoo across multiple repositories",
) {
    private lateinit var odooLegacyRepository: LegacyRepository
    private lateinit var enterpriseLegacyRepository: LegacyRepository
    private lateinit var state: BisectState

    override fun run() {
        val workspace = workspaces.current() ?: throw Abort()

        val steps = runbot.batches(workspace.base)
        this.state = BisectState(low = 0, high = steps.lastIndex, steps, workspace)

        with(git) {
            odooLegacyRepository = workspace.odoo()
            enterpriseLegacyRepository = workspace.enterprise()
        }

        draw()

        while (state.low <= state.high) {
            var (low, mid, high) = state
            while (true) {
                when (terminal.readLineOrNull(hideInput = false)) {
                    "good" -> {
                        high = mid - 1
                        break
                    }

                    "bad" -> {
                        low = mid + 1
                        break
                    }

                    null -> {
                        terminal.println()
                        throw Abort()
                    }

                    else -> {
                        terminal.cursor.move {
                            up(1)
                            clearLineAfterCursor()
                        }
                        terminal.print(prompt)
                    }
                }
            }

            if (high == -1) {
                TODO("Every commit is good")
            }

            if (low == high) {
                break
            }

            state = BisectState(low = low, high = high, batches = steps, workspace = workspace)
            draw()
        }

        terminal.cursor.move {
            up(3)
            startOfLine()
            clearLineAfterCursor()
            clearScreenAfterCursor()
        }

        val firstGood = steps[state.high + 1]
        val firstBad = steps[state.high]

        // TODO: bisect individual commits
        terminal.print(
            buildString {
                appendLine((bold + green)("Possibly bad odoo commits:"))
                odooLegacyRepository.commitsBetween(firstGood.odoo, firstBad.odoo)
                    .forEach {
                        append(yellow(it.hash))
                        append(' ')
                        appendLine(it.title)
                    }
                appendLine()
                appendLine((bold + green)("Possibly bad enterprise commits:"))
                enterpriseLegacyRepository.commitsBetween(firstGood.enterprise, firstBad.enterprise)
                    .forEach {
                        append(yellow(it.hash))
                        append(' ')
                        appendLine(it.title)
                    }
            },
        )
    }

    var first = true
    private val prompt by lazy { terminal.theme.style("prompt.default")("good | bad ? ") }

    private fun draw() {
        val odooTitle = odooLegacyRepository.commitTitle(state.batch.odoo)
        val enterpriseTitle = enterpriseLegacyRepository.commitTitle(state.batch.enterprise)

        val width = terminal.info.width
        val total = state.batches.size

        val ratio = (total.toDouble()) / width

        // FIXME: rounding
        val start = (state.low / ratio).roundToInt()
        val middle = ((state.high - state.low) / ratio).roundToInt()
        val end = (width - (start + middle))

        assert(start + middle + end == width)

        if (!first) {
            terminal.cursor.move {
                up(4)
                clearScreenAfterCursor()
            }
        }

        terminal.print(
            buildString {
                append((black on yellow)("odoo"))
                append(' ')
                append(yellow(state.batch.odoo))
                append(' ')
                append(odooTitle)
                appendLine()

                append((black on yellow)("enterprise"))
                append(' ')
                append(yellow(state.batch.enterprise))
                append(' ')
                append(enterpriseTitle)
                appendLine()

                append(terminal.theme.style("danger")(String(CharArray(start) { '━' })))
                append(terminal.theme.style("muted")(String(CharArray(middle) { '━' })))
                append(terminal.theme.style("success")(String(CharArray(end) { '━' })))
                appendLine()

                append(magenta("Switching branches.."))
            },
        )

        odooLegacyRepository.switch(state.batch.odoo)
        enterpriseLegacyRepository.switch(state.batch.enterprise)

        terminal.cursor.move {
            clearLineBeforeCursor()
            startOfLine()
        }
        terminal.print(prompt)

        first = false
    }
}
