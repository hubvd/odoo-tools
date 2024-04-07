package com.github.hubvd.odootools.worktree

import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.hubvd.odootools.workspace.Workspace
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlin.io.path.div

enum class Repository(val url: String, val pathName: String) {
    Odoo("git@github.com:odoo/odoo.git", "odoo"),
    Enterprise("git@github.com:odoo/enterprise.git", "enterprise"),
    DesignThemes("git@github.com:odoo/design-themes.git", "design-themes"),
    Stubs("git@github.com:odoo-ide/odoo-stubs.git", "odoo-stubs"),
}

fun odooRepositories() = listOf(
    Repository.Odoo,
    Repository.Enterprise,
    Repository.DesignThemes,
)

context(ProcessSequenceDslContext)
suspend fun createGitWorktrees(root: Workspace, target: Workspace, base: String, name: String) {
    odooRepositories().forEach { repository ->
        val branchExists = process(
            "git",
            "-C",
            "${root.path / repository.pathName}",
            "show-branch",
            "remotes/origin/$base",
            stdout = Redirect.SILENT,
            stderr = Redirect.SILENT,
        ).resultCode == 0

        if (!branchExists) {
            run(
                "git",
                "-C",
                "${root.path / repository.pathName}",
                "fetch",
                "origin",
                base,
                description = "${bold(repository.pathName)}: Fetching ${green(base)} ",
            )
        }

        run(
            "git",
            "-C",
            "${root.path / repository.pathName}",
            "worktree",
            "add",
            "-B",
            name,
            if (name == base) "--track" else "--no-track",
            "${target.path / repository.pathName}",
            "origin/$base",
            description = "${bold(repository.pathName)}: Creating worktree",
        )
    }
}

context(ProcessSequenceDslContext)
suspend fun pruneGitWorktrees(root: Workspace) = odooRepositories().forEach { repository ->
    run(
        "git",
        "-C",
        "${root.path / repository.pathName}",
        "worktree",
        "prune",
        description = "${bold(repository.pathName)}: Pruning worktrees",
    )
}
