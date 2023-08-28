package com.github.hubvd.odootools.workspace

class FakeWorkspaces(
    private val workspaces: List<StaticWorkspace>,
    private val default: String,
    private val current: String?,
) : Workspaces {
    override fun list(): List<Workspace> = workspaces

    override fun default(): Workspace = workspaces.first { it.name == default }

    override fun current(): Workspace? = workspaces.find { it.name == current }
}
