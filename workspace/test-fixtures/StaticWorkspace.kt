package com.github.hubvd.odootools.workspace

import java.nio.file.Path

data class StaticWorkspace(
    override val name: String,
    override val path: Path,
    override val version: Float,
    override val base: String,
) : Workspace
