package com.github.hubvd.odootools.worktree

import com.github.hubvd.odootools.workspace.Workspace
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
suspend fun createGitWorktrees(root: Workspace, target: Workspace, base: String) {
    odooRepositories().forEach { repository ->
        run(
            "git",
            "-C",
            "${root.path / repository.pathName}",
            "fetch",
            "origin",
            base,
        )
        run(
            "git",
            "-C",
            "${root.path / repository.pathName}",
            "worktree",
            "add",
            "-f",
            "${target.path / repository.pathName}",
            "origin/$base",
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
    )
}
