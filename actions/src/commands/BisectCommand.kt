package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.input.interactiveSelectList
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.hubvd.odootools.actions.utils.*
import com.github.hubvd.odootools.workspace.Workspaces
import kotlin.io.path.div
import kotlin.math.ceil
import kotlin.math.log2

enum class BisectPromptResult {
    Good,
    Bad,
    Exit,
}

sealed class BisectResult {
    data object Abort : BisectResult()
    data object NoBadBatches : BisectResult()
    data object NoGoodBatches : BisectResult()

    /**
     * Let's say the bug was visible witch batch 1 but not on batch 2
     * It could have been introduced in commit A or B (they both came from batch 1)
     *
     * 9d184b7de626 A 1
     * e6aba8d43449 B
     * 71c2c5d52988 C 2
     *
     * In this case, we return Found(firstGood=2, firstBad=1)
     */
    data class Found(val firstGood: ResolvedBatch, val firstBad: ResolvedBatch) : BisectResult()
}

class Bisect(
    private val batches: List<ResolvedBatch>,
    private val prompt: () -> BisectPromptResult,
    private val printState: (batch: ResolvedBatch, remainingSteps: Int) -> Unit,
    private val commitState: (batch: ResolvedBatch) -> Unit,
) {

    private val totalSteps = ceil(log2(batches.size.toDouble())).toInt()
    private var steps = 0

    private fun advance(batch: ResolvedBatch) {
        printState(batch, totalSteps - steps)
        commitState(batch)
        steps++
    }

    operator fun invoke(): BisectResult {
        var aborted = false
        val index = batches.binarySearch {
            advance(it)
            when (prompt()) {
                BisectPromptResult.Good -> -1
                BisectPromptResult.Bad -> 1
                BisectPromptResult.Exit -> {
                    aborted = true
                    0
                }
            }
        }

        if (aborted) {
            return BisectResult.Abort
        }

        assert(index < 0)

        // Since we never return 0, the index will always be negative
        // Revert it to get the insertion point
        return when (val insertionPoint = (index + 1) * -1) {
            0 -> BisectResult.NoGoodBatches
            batches.size -> BisectResult.NoBadBatches
            else -> BisectResult.Found(
                firstGood = batches[insertionPoint + 1],
                firstBad = batches[insertionPoint],
            )
        }
    }
}

class BisectCommand(
    private val runbot: Runbot,
    private val workspaces: Workspaces,
    private val git: Git,
) : CliktCommand() {
    override fun help(context: Context) = "Bisect odoo across multiple repositories"

    private lateinit var odooLegacyRepository: LegacyRepository
    private lateinit var enterpriseLegacyRepository: LegacyRepository

    private val goodTerm by option("--term-good").default("good")
    private val badTerm by option("--term-bad").default("bad")

    var first: Boolean = true

    override fun run() {
        val workspace = workspaces.current() ?: throw Abort()
        val batches = runbot.batches(workspace.base)

        odooLegacyRepository = git.open(workspace.path / "odoo")
        enterpriseLegacyRepository = git.open(workspace.path / "enterprise")

        val result = Bisect(
            batches,
            prompt = {
                when (
                    terminal.interactiveSelectList(
                        listOf(goodTerm, badTerm),
                        title = terminal.theme.style("prompt.default")("$goodTerm | $badTerm ? "),
                    )
                ) {
                    goodTerm -> BisectPromptResult.Good
                    badTerm -> BisectPromptResult.Bad
                    else -> BisectPromptResult.Exit
                }
            },
            printState = { batch, rem ->
                printState(batch, rem)
                first = false
            },
            commitState = { batch ->
                terminal.print(magenta("Switching branches.."))

                // TODO: retries if switch fails
                odooLegacyRepository.switch(batch.odoo)
                enterpriseLegacyRepository.switch(batch.enterprise)

                terminal.cursor.move {
                    clearLineBeforeCursor()
                    startOfLine()
                }
            },
        )()

        clearState()
        when (result) {
            BisectResult.Abort -> {
                throw Abort()
            }

            BisectResult.NoBadBatches -> {
                terminal.println(terminal.theme.style("success")("No bad batches found"))
            }

            BisectResult.NoGoodBatches -> {
                terminal.println(terminal.theme.style("danger")("No good batches found"))
            }

            is BisectResult.Found -> {
                // TODO: bisect individual commits
                terminal.print(
                    buildString {
                        appendLine((bold + green)("odoo commits candidates:"))
                        odooLegacyRepository.commitsBetween(result.firstGood.odoo, result.firstBad.odoo)
                            .forEach {
                                append(yellow(it.hash))
                                append(' ')
                                appendLine(it.title)
                            }
                        appendLine()
                        appendLine((bold + green)("enterprise commits candidates:"))
                        enterpriseLegacyRepository.commitsBetween(
                            result.firstGood.enterprise,
                            result.firstBad.enterprise,
                        )
                            .forEach {
                                append(yellow(it.hash))
                                append(' ')
                                appendLine(it.title)
                            }
                    },
                )
            }
        }
    }

    private fun clearState() {
        if (!first) {
            terminal.cursor.move {
                up(3)
                clearScreenAfterCursor()
            }
        }
    }

    private fun printState(batch: ResolvedBatch, remainingSteps: Int) {
        val odooTitle = odooLegacyRepository.commitTitle(batch.odoo)
        val enterpriseTitle = enterpriseLegacyRepository.commitTitle(batch.enterprise)

        clearState()

        terminal.print(
            buildString {
                append((black on yellow)("odoo"))
                append(' ')
                append(yellow(batch.odoo))
                append(' ')
                append(odooTitle)
                appendLine()

                append((black on yellow)("enterprise"))
                append(' ')
                append(yellow(batch.enterprise))
                append(' ')
                append(enterpriseTitle)
                appendLine()

                append(terminal.theme.style("muted")("Remaining steps: $remainingSteps"))
                appendLine()
            },
        )
    }
}
