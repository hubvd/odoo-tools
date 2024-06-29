package com.github.hubvd.odootools.actions.utils

import java.nio.file.Path

class FakeGit(private val logs: Map<Path, String>) : Git {
    override fun open(path: Path) = FakeLegacyRepository(logs[path]!!)
}
