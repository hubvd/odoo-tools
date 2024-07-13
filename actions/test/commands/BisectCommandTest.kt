@file:Suppress("ktlint:standard:max-line-length")

package com.github.hubvd.odootools.actions.commands

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.hubvd.odootools.actions.utils.*
import org.junit.jupiter.api.Test
import org.kodein.di.*
import kotlin.io.path.div

class BisectCommandTest {

    private val batches = listOf(
        ResolvedBatch("f6c2b5368e3abdc292e66dc04efc4031eb12f742", "b7a27ca150287ab66fb8fc0faa4da5b29b61508e"),
        ResolvedBatch("f6c2b5368e3abdc292e66dc04efc4031eb12f742", "59ebb6c669c543bdf79cbee31f7d18d0cadb6410"),
        ResolvedBatch("0a16e7f7b49b94f5b74544dc96dfb7a057f587b5", "59ebb6c669c543bdf79cbee31f7d18d0cadb6410"),
        ResolvedBatch("092241c33980f9d15979c268ad7a7832bca700e9", "404458a3e804ceaab5cba4c6fc54aa1db1675cda"),
        ResolvedBatch("092241c33980f9d15979c268ad7a7832bca700e9", "dfd6ffe5abd7dff4e2f432e0b17719967b9a1a36"),
    )

    @Test
    fun allGood() {
        val bisect = Bisect(
            batches,
            { BisectPromptResult.Good },
            { _, _ -> },
            {},
        )
        assertThat(bisect()).isEqualTo(BisectResult.NoBadBatches)
    }

    @Test
    fun allBad() {
        val bisect = Bisect(
            batches,
            { BisectPromptResult.Bad },
            { _, _ -> },
            {},
        )
        assertThat(bisect()).isEqualTo(BisectResult.NoGoodBatches)
    }

    @Test
    fun mixed() {
        var currentBatch: ResolvedBatch? = null
        val bisect = Bisect(
            batches,
            {
                if (batches.indexOf(currentBatch!!) >= 2) {
                    BisectPromptResult.Bad
                } else {
                    BisectPromptResult.Good
                }
            },
            { _, _ -> },
            { currentBatch = it },
        )
        assertThat(bisect()).isEqualTo(
            BisectResult.Found(
                firstGood = batches[3],
                firstBad = batches[2],
            ),
        )
    }

    @Test
    fun mixed2() {
        val answers = mutableListOf(
            BisectPromptResult.Bad,
            BisectPromptResult.Bad,
            BisectPromptResult.Good,
        )
        val bisect = Bisect(
            batches,
            { answers.removeLast() },
            { _, _ -> },
            { },
        )
        assertThat(bisect()).isEqualTo(
            BisectResult.Found(
                firstGood = batches[4],
                firstBad = batches[3],
            ),
        )
    }
}
