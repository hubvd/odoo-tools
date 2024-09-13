package com.github.hubvd.odootools.pycharmctl.plugin

import com.github.hubvd.odootools.pycharmctl.api.PycharmCtl
import com.github.hubvd.odootools.pycharmctl.api.PycharmProject
import com.github.hubvd.odootools.pycharmctl.api.PycharmProject.PycharmProjectByPath
import com.github.hubvd.odootools.pycharmctl.api.PycharmProject.PycharmProjectInfo
import com.github.hubvd.odootools.pycharmctl.api.ShellPath
import com.intellij.ide.GeneralSettings
import com.intellij.ide.GeneralSettings.Companion.OPEN_PROJECT_NEW_WINDOW
import com.intellij.ide.GeneralSettings.Companion.OPEN_PROJECT_SAME_WINDOW
import com.intellij.ide.RecentProjectMetaInfo
import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.WindowManager
import com.intellij.platform.ide.progress.ModalTaskOwner.project
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.io.path.Path

class PycharmCtlImpl : PycharmCtl {

    private val projectManager get() = ProjectManager.getInstance()

    override fun openedProjects(): List<PycharmProjectInfo> {
        val recentProjectsManager = RecentProjectsManager.getInstance() as? RecentProjectsManagerBase ?: TODO()
        val info = recentProjectsManager.state.additionalInfo

        // latest first
        val projectsSortedByLastActivation =
            projectManager.openProjects.filter { it.presentableUrl != null }.sortedByDescending {
                val projectPath = FileUtilRt.toSystemIndependentName(it.presentableUrl!!)
                info.computeIfAbsent(projectPath) { RecentProjectMetaInfo() }.activationTimestamp
            }

        return projectsSortedByLastActivation.map {
            PycharmProjectInfo(name = it.name, path = Path(it.basePath ?: ""))
        }
    }

    override fun currentProject(): PycharmProjectInfo = openedProjects().first()

    override fun focus(project: PycharmProject) {
        val project = projectManager.findOpen(project) ?: return
        val frame = WindowManager.getInstance().allProjectFrames.find { it.project == project } ?: return
        val window = SwingUtilities.getWindowAncestor(frame.component) ?: return
        window.toFront()
        window.requestFocus()
    }

    override fun openProjectInCurrentWindow(project: PycharmProjectByPath) {
        val jetbrainsProject = projectManager.findOpen(project)
        if (jetbrainsProject != null) {
            focus(project)
        } else {
            openProject(project.path, false)
        }
    }

    override fun openProjectInNewWindow(project: PycharmProjectByPath) {
        val jetbrainsProject = projectManager.findOpen(project)
        if (jetbrainsProject != null) {
            focus(project)
        } else {
            openProject(project.path, true)
        }
    }

    override fun openFile(path: ShellPath) {
        val matchedProject = projectManager.openProjects.find { path.startsWith(Path(it.basePath ?: "")) } ?: return
        val vf = LocalFileSystem.getInstance().findFileByNioFile(path)
            ?: return
        ApplicationManager.getApplication().invokeLater {
            PsiNavigationSupport.getInstance().createNavigatable(matchedProject, vf, -1).navigate(true)
        }
    }

    private fun ProjectManager.findOpen(project: PycharmProject): Project? = openProjects.find {
        when (project) {
            is PycharmProject.PycharmProjectByName -> it.name == project.name
            is PycharmProject.PycharmProjectByPath -> Path(it.basePath ?: "") == project.path
            is PycharmProjectInfo -> Path(it.basePath ?: "") == project.path && it.name == project.name
        }
    }

    private fun openProject(path: Path, newWindow: Boolean) {
        val settings = GeneralSettings.getInstance()
        val oldValue = settings.confirmOpenNewProject
        try {
            if (!newWindow) {
                focus(currentProject())
            }
            settings.confirmOpenNewProject = if (newWindow) OPEN_PROJECT_NEW_WINDOW else OPEN_PROJECT_SAME_WINDOW
            ProjectManagerEx.getInstanceEx()
                .openProject(path, OpenProjectTask.build().withForceOpenInNewFrame(newWindow))
        } finally {
            settings.confirmOpenNewProject = oldValue
        }
    }
}
