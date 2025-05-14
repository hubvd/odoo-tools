package com.github.hubvd.odootools.actions.commands.repo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.colormath.model.RGB
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.table.horizontalLayout
import com.github.ajalt.mordant.table.verticalLayout
import com.github.hubvd.odootools.libgit.legacy.Repository
import com.github.hubvd.odootools.libgit.legacy.git_branch_t
import com.github.hubvd.odootools.workspace.Workspace
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

class StatusCommand : CliktCommand() {
    private val showDirty by option("-d", "--dirty", "--show-dirty").flag()
    private val workspaceRepositories by requireObject<List<WorkspaceRepositories>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun run() = runBlocking {
        val res = workspaceRepositories.map {
            async(it.dispatcher) {
                it.workspace to arrayOf(
                    line(it.odoo.await(), it.workspace.base),
                    line(it.enterprise.await(), it.workspace.base),
                    line(it.designThemes.await(), it.workspace.base),
                )
            }
        }.awaitAll()

        val sectionStyle = bold + TextStyle(RGB("#61afef"))

        terminal.println(
            verticalLayout {
                spacing = 1
                res.forEach { (workspace, repos) ->
                    val (odoo, enterprise, designThemes) = repos
                    cell(workspaceSection(sectionStyle, workspace, odoo, enterprise, designThemes))
                }
            },
        )
    }

    private fun workspaceSection(
        sectionStyle: TextStyle,
        workspace: Workspace,
        odoo: Widget,
        enterprise: Widget,
        designThemes: Widget,
    ) = horizontalLayout {
        cell(sectionStyle(Array(4) { "│" }.joinToString("\n")))
        cell(
            verticalLayout {
                cells(
                    bold(workspace.name),
                    odoo,
                    enterprise,
                    designThemes,
                )
            },
        )
    }

    private fun Pair<Long, Long>.formatAheadBehind(style: TextStyle, name: String? = null): String = style(
        buildString {
            append('(')
            name?.let { append(name).append(": ") }
            second.let { append("↓").append(it) }
            first.let { append(" ↑").append(it) }
            append(')')
        },
    )

    private fun line(repository: Repository, base: String) = repository.let {
        horizontalLayout {
            val head = it.head()

            cell(
                buildString {
                    val headName = if (head.isBranch()) {
                        head.branchName()
                    } else {
                        it.shortId(head)
                    }
                    append(gray(headName!!))
                    if (showDirty && it.status().count() > 0) {
                        append(' ')
                        append(red("•"))
                    }
                },
            )

            cell(
                buildString {
                    val upstream = head.upstream()?.target()
                    if (upstream != null) {
                        append(head.target()!!.aheadBehind(upstream).formatAheadBehind(green))
                    } else {
                        append(red("??"))
                    }

                    val baseUpstream = it.findBranch("origin/$base", git_branch_t.GIT_BRANCH_REMOTE)?.target()
                    if (baseUpstream != null && (upstream == null || !upstream.isEqual(baseUpstream))) {
                        append(' ')
                        append(head.target()!!.aheadBehind(baseUpstream).formatAheadBehind(yellow, name = base))
                    }
                },
            )
        }
    }
}
