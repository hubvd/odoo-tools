package com.github.hubvd.odootools.actions.commands

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.defaultLazy
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.hubvd.odootools.actions.utils.BrowserService
import com.github.hubvd.odootools.actions.utils.Clipboard

class OpenerCommand(private val browserService: BrowserService) : CliktCommand() {
    override fun help(context: Context) = "Opens a PR/Task"

    private val input by argument().defaultLazy { Clipboard.read(selection = true) }

    private val type by mutuallyExclusiveOptions(
        option("-t", "--ticket").flag().convert { Type.Task },
        option("-p", "--pr", "--pull-request").flag().convert { Type.PullRequest },
        option("-r", "--runbot").flag().convert { Type.RunbotError },
    ).single()

    private val all by option("-a", "--all").flag()

    override fun run() {
        if (all) {
            return browserService.open((type ?: Type.Task).allUrl)
        }

        type?.run {
            resolveUrl(input)?.let { return browserService.open(it) }
            throw Abort()
        }

        for (t in Type.entries) {
            t.resolveUrl(input)?.let { return browserService.open(it) }
        }

        throw Abort()
    }

    private enum class Type(
        val allUrl: String,
        private val regexes: List<Regex>,
        private val extractor: (MatchResult) -> String?,
    ) {
        PullRequest(
            allUrl = "https://github.com/pulls",
            regexes = listOf(Regex("""odoo/(?<repo>.*)[#:](?<id>\d+)""")),
            extractor = { m ->
                val (repo, id) = m.destructured
                "https://github.com/odoo/$repo/pull/$id"
            },
        ),
        Task(
            allUrl = "https://www.odoo.com/odoo/49/tasks",
            regexes = listOf(Regex("""(?i)(opw|task)[\W\-]*(?:id)?[\W-]*(?<id>\d+)"""), NUMERICAL_ID_RE),
            extractor = { m -> m.groups["id"]?.value?.let { "https://www.odoo.com/odoo/49/tasks/$it" } },
        ),
        RunbotError(
            allUrl = "https://runbot.odoo.com/odoo/error",
            regexes = listOf(Regex("""(?i)runbot[\W\-]*(?:id)?[\W-]*(?<id>\d+)"""), NUMERICAL_ID_RE),
            extractor = { m -> m.groups["id"]?.value?.let { "https://runbot.odoo.com/odoo/runbot.build.error/$it" } },
        ),
        ;

        fun resolveUrl(input: String): String? =
            regexes.firstNotNullOfOrNull { regex -> regex.find(input)?.let(extractor) }
    }

    companion object {
        private val NUMERICAL_ID_RE = Regex("""^(?<id>\d+)$""")
    }
}
