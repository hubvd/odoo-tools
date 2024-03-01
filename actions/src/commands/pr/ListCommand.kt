package com.github.hubvd.odootools.actions.commands.pr

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.MissingOption
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.colormath.model.RGB
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.Companion.hyperlink
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.table.horizontalLayout
import com.github.ajalt.mordant.table.verticalLayout
import com.github.ajalt.mordant.widgets.EmptyWidget
import com.github.ajalt.mordant.widgets.Panel
import com.github.ajalt.mordant.widgets.Text
import com.github.hubvd.odootools.actions.ActionsConfig
import com.github.hubvd.odootools.actions.utils.*

class ListCommand(private val github: GithubClient, private val config: ActionsConfig) : CliktCommand(
    help = "List pull requests involved with the selected username",
) {
    private val closed by option().flag()
    private val githubUsername by option().defaultLazy {
        config.githubUsernames[odooUsername] ?: throw MissingOption(
            registeredOptions().find { it.names.contains("--github-username") }!!,
        )
    }
    private val odooUsername by option().default(config.trigram)
    private val title by argument().optional()

    override fun run() {
        val terminal = currentContext.terminal
        terminal.print(brightBlue("Fetching pull requests.."))

        val prResult = github.findPullRequests(
            username = if (title == null) githubUsername else null,
            open = !closed,
            title = title,
        )

        terminal.cursor.move {
            clearLine()
            startOfLine()
        }

        if (title != null) {
            val prs = prResult.fold(
                { throw PrintMessage(it.toString(), statusCode = 1) },
                { it },
            )
            with(terminal) {
                println(pullRequestGroupWidget("Pull Requests", prs))
            }
            return
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
                pullRequests
                    .groupBy { it.parent() ?: it.id }
                    .values
                    .map { it.sortedBy { it.id } }
                    .forEach { cell(pullRequestGroupWidget(it)) }
            },
            title = Text(title),
            titleAlign = TextAlign.LEFT,
            borderStyle = gray,
            expand = true,
        )
    }

fun pullRequestGroupWidget(pullRequests: List<PullRequest>): Widget {
    if (pullRequests.size == 1) {
        return pullRequestWidget(pullRequests.first()).first
    }

    return verticalLayout {
        spacing = 0
        cell(pullRequestWidget(pullRequests.first()).first)
        pullRequests.drop(1).forEachIndexed { index, request ->
            cell(
                horizontalLayout {
                    val (content, lines) = pullRequestWidget(request)
                    if (index == pullRequests.size - 2) {
                        cell("└──")
                    } else {
                        cell(
                            buildString {
                                append("├──\n")
                                repeat(lines - 1) {
                                    append("│\n")
                                }
                                delete(length - 1, length)
                            },
                        )
                    }
                    cell(content)
                },
            )
        }
    }
}

fun pullRequestWidget(pullRequest: PullRequest): Pair<Text, Int> {
    val color = when (pullRequest.state()) {
        CheckState.FAILURE, CheckState.ERROR -> red
        CheckState.SUCCESS -> green
        CheckState.PENDING -> blue
    }
    val normalizedTitle = pullRequest.normalizedTitle()
    var (_, tags, title) = TAG_RE.find(normalizedTitle)?.groupValues ?: listOf("", "", normalizedTitle)
    if (pullRequest.isInConflict()) {
        tags += "[CONFLICT]"
    }

    val text = buildString {
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
    }
    return Text(text) to text.count { it == '\n' } + 1
}

private val TAG_STYLE = TextStyle(RGB("#61afef"))
private val TAG_RE = Regex("""^(\[\S*])(.*)$""")
