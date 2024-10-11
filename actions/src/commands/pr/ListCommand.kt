package com.github.hubvd.odootools.actions.commands.pr

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.groups.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.colormath.model.RGB
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.Companion.hyperlink
import com.github.ajalt.mordant.rendering.TextStyles.underline
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.table.horizontalLayout
import com.github.ajalt.mordant.table.verticalLayout
import com.github.ajalt.mordant.widgets.EmptyWidget
import com.github.ajalt.mordant.widgets.HorizontalRule
import com.github.ajalt.mordant.widgets.Text
import com.github.hubvd.odootools.actions.ActionsConfig
import com.github.hubvd.odootools.actions.utils.*

class ListCommand(
    private val github: GithubClient,
    private val employeeService: EmployeeService,
    config: ActionsConfig,
) : CliktCommand() {
    override fun help(context: Context) = "List pull requests involved with the selected username"

    private val closed by option("-c", "--closed").flag()
    private val involved by option("-i", "--involved", "-r", "--reviews").flag()
    private val user by userIdOption().default(User.OdooUser(config.trigram))
    private val title by argument().optional()

    override fun run() {
        val terminal = currentContext.terminal
        terminal.print(brightBlue("Fetching pull requests.."))

        val odooUser: User.OdooUser
        val githubUser: User.GithubUser
        when (val userId = user) {
            is User.GithubUser -> {
                githubUser = userId
                odooUser = employeeService[userId] ?: throw Abort()
            }

            is User.OdooUser -> {
                odooUser = userId
                githubUser = employeeService[userId] ?: throw Abort()
            }
        }

        val prResult = github.findPullRequests(
            username = if (title == null) githubUser.username else null,
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
                println(pullRequestGroupSectionWidget(prs))
            }
            return
        }

        val (ownPrs, involvedPrs) = prResult.fold(
            { throw PrintMessage(it.toString(), statusCode = 1) },
            { it.partition { odooUser.username in it.headRefName } },
        )
        with(terminal) {
            if (involved) {
                println(HorizontalRule("Pull Requests", ruleStyle = gray))
            }
            println(pullRequestGroupSectionWidget(ownPrs))
            if (involved) {
                println(HorizontalRule("Reviews", ruleStyle = gray))
                println(pullRequestGroupSectionWidget(involvedPrs))
            }
        }
    }
}

private fun pullRequestGroupSectionWidget(pullRequests: List<PullRequest>): Widget = if (pullRequests.isEmpty()) {
    EmptyWidget
} else {
    verticalLayout {
        spacing = 1
        pullRequests
            .groupBy { it.parent() ?: it.id }
            .values
            .map { it.sortedBy { it.id } }
            .forEach { cell(pullRequestGroupWidget(it)) }
    }
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

        var branch = pullRequest.headRefName
        TASK_RE.find(branch)?.let { match ->
            branch = branch.replaceRange(match.range, magenta(match.value))
        }

        END_RE.find(branch)?.let { match ->
            val idMatch = match.groups["id"]!!
            val newId = underline(idMatch.value)
            branch = branch.replaceRange(idMatch.range, newId)
            match.groups["fw"]?.let { group ->
                val offset = newId.length - idMatch.value.length
                branch = branch.replaceRange(
                    group.range.start + offset,
                    group.range.endInclusive + 1 + offset,
                    gray(group.value),
                )
            }
        }

        TARGET_RE.find(branch)?.let { match ->
            branch = branch.replaceRange(match.range, cyan(match.value))
        }
        append(branch)
    }
    return Text(text) to text.count { it == '\n' } + 1
}

private val TASK_RE = Regex("""(?:task|opw|sentry)-\d+""")
private val END_RE = Regex("""(?<id>[a-z]{2,4})-?(?<fw>[a-zA-Z0-9_-]{4}-fw)?$""")
private val TARGET_RE = Regex("""(?:master|(?:saas-)?\d{2}\.\d)(?:-(?:saas-)?\d{2}\.\d)?""")
private val TAG_STYLE = TextStyle(RGB("#61afef"))
private val TAG_RE = Regex("""^(\[\S*])(.*)$""")
