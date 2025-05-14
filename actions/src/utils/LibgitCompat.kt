package com.github.hubvd.odootools.actions.utils

import com.github.hubvd.odootools.libgit.legacy.Repository
import com.github.hubvd.odootools.workspace.Workspace
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists

fun Workspace.currentRepositoryPath(): Path? = path.relativize(Path(System.getProperty("user.dir")))
    .subpath(0, 1)
    .takeIf { it.toString().isNotEmpty() }
    ?.let { path / it }
    ?.takeIf { (it / ".git").exists() }

fun Workspace.currentRepository(): Repository? = currentRepositoryPath()?.let { Repository.open(it) }
