package com.github.hubvd.odootools.pycharmctl.api

import com.github.hubvd.odootools.pycharmctl.api.PycharmProject.PycharmProjectByPath

interface PycharmCtl {

    fun openedProjects(): List<PycharmProject.PycharmProjectInfo>

    fun currentProject(): PycharmProject.PycharmProjectInfo

    fun focus(project: PycharmProject)

    fun openProjectInCurrentWindow(project: PycharmProjectByPath)

    fun openProjectInNewWindow(project: PycharmProjectByPath)

    fun openFile(path: ShellPath)

    companion object
}
