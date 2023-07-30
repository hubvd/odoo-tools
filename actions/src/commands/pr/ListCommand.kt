package com.github.hubvd.odootools.actions.commands.pr

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.colormath.model.RGB
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.Companion.hyperlink
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.table.verticalLayout
import com.github.ajalt.mordant.widgets.EmptyWidget
import com.github.ajalt.mordant.widgets.Panel
import com.github.ajalt.mordant.widgets.Text
import com.github.hubvd.odootools.actions.utils.CheckState
import com.github.hubvd.odootools.actions.utils.GithubClient
import com.github.hubvd.odootools.actions.utils.PullRequest
import com.github.hubvd.odootools.actions.utils.state

class ListCommand(private val github: GithubClient) : CliktCommand(
    help = "List pull requests involved with the selected username",
) {
    private val githubUsername by option().default("hubvd")
    private val odooUsername by option().default("huvw")

    override fun run() {
        val terminal = currentContext.terminal
        terminal.print(brightBlue("Fetching pull requests.."))

        val prResult = github.findPullRequestsInvolving(githubUsername)
        terminal.cursor.move {
            clearLine()
            startOfLine()
        }
        val (ownPrs, involvedPrs) = prResult.fold(
            { throw PrintMessage(it.toString(), statusCode = 1) },
            { it.partition { odooUsername in it.headRefName } },
        )
        with(terminal) {
            println(pullRequestGroupWidget("Pull Requests", ownPrs))
            println(pullRequestGroupWidget("Reviews", involvedPrs))
        }
    }
}

private fun pullRequestGroupWidget(title: String, pullRequests: List<PullRequest>): Widget =
    if (pullRequests.isEmpty()) {
        EmptyWidget
    } else {
        Panel(
            verticalLayout {
                spacing = 1
                pullRequests.forEach {
                    cell(pullRequestWidget(it))
                }
            },
            title = Text(title),
            titleAlign = TextAlign.LEFT,
            borderStyle = gray,
            expand = true,
        )
    }

fun pullRequestWidget(pullRequest: PullRequest): Widget {
    val color = when (pullRequest.state()) {
        CheckState.FAILURE, CheckState.ERROR -> red
        CheckState.SUCCESS -> green
        CheckState.PENDING -> blue
    }
    val normalizedTitle = pullRequest.normalizedTitle()
    val (_, tags, title) = TAG_RE.find(normalizedTitle)?.groupValues ?: listOf("", "", normalizedTitle)

    return Text(
        buildString {
            appendLine(hyperlink(pullRequest.url)(TAG_STYLE(tags) + color(title)) + ' ')
            pullRequest.checks
                .filterNot { it.context == "ci/codeowner" }
                .filter { it.state == CheckState.FAILURE }
                .forEach {
                    append(TAG_STYLE('[' + it.context + ']'))
                    appendLine(red(" " + it.targetUrl))
                }
            pullRequest.checks
                .filter { it.state == CheckState.PENDING }
                .forEach {
                    append(TAG_STYLE('[' + it.context + ']'))
                    appendLine(yellow(" " + it.targetUrl))
                }
            append(pullRequest.headRefName)
        },
    )
}

private val TAG_STYLE = TextStyle(RGB("#61afef"))
private val TAG_RE = Regex("""^(\[\S*])(.*)$""")
