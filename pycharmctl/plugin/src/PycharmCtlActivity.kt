package com.github.hubvd.odootools.pycharmctl.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class PycharmCtlActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        PycharmCtlService
    }
}
