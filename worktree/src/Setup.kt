package com.github.hubvd.odootools.worktree

import com.github.ajalt.clikt.core.CliktError
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.name

private fun repositories(root: Path, community: Boolean = false) =
    sequenceOf("odoo", "enterprise")
        .filter { if (community) it != "enterprise" else true }
        .map { root / "master" / it }
        .filter { (it / ".git").exists() }
        .map { it.name }
        .toList()
        .also { if ("odoo" !in it) throw CliktError("Odoo repository not found in ${root / "master"}") }

context(ProcessSequenceDslContext)
suspend fun createGitWorktrees(
    root: Path,
    path: Path,
    base: String,
    community: Boolean = false,
) = repositories(root, community).forEach { repo ->
    run(
        "git",
        "-C",
        "${root / "master" / repo}",
        "fetch",
        "origin",
        base
    )

    run(
        "git",
        "-C",
        "${root / "master" / repo}",
        "worktree",
        "add",
        "-f",
        "${path / repo}",
        "origin/$base",
    )
}

context(ProcessSequenceDslContext)
suspend fun pruneGitWorktrees(root: Path) = repositories(root).forEach { repo ->
    run(
        "git",
        "-C",
        "${root / "master" / repo}",
        "worktree",
        "prune",
    )
}
