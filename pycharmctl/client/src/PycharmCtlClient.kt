package com.github.hubvd.odootools.pycharmctl.client

import com.github.hubvd.odootools.pycharmctl.api.PycharmCtl
import com.github.hubvd.odootools.pycharmctl.api.PycharmProject
import com.github.hubvd.odootools.pycharmctl.api.ShellPath
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters

class PycharmCtlClient(httpClient: HttpHandler) : PycharmCtl {

    private val http = ClientFilters.SetBaseUriFrom(Uri.of("http://localhost:8000")).then(httpClient)
    private val json = Json

    override fun openedProjects() = json.decodeFromString(
        ListSerializer(PycharmProject.PycharmProjectInfo.serializer()),
        http(Request(Method.GET, "/projects")).bodyString(),
    )

    override fun currentProject() = json.decodeFromString(
        PycharmProject.PycharmProjectInfo.serializer(),
        http(Request(Method.GET, "/project")).bodyString(),
    )

    override fun focus(project: PycharmProject) {
        http(
            Request(Method.POST, "/focus").body(
                json.encodeToString(PycharmProject.serializer(), project),
            ),
        )
    }

    override fun openProjectInCurrentWindow(project: PycharmProject.PycharmProjectByPath) {
        http(
            Request(Method.POST, "/open").body(
                json.encodeToString(PycharmProject.PycharmProjectByPath.serializer(), project),
            ).query("new", "false"),
        )
    }

    override fun openProjectInNewWindow(project: PycharmProject.PycharmProjectByPath) {
        http(
            Request(Method.POST, "/open").body(
                json.encodeToString(PycharmProject.PycharmProjectByPath.serializer(), project),
            ).query("new", "true"),
        )
    }

    override fun openFile(path: ShellPath) {
        http(
            Request(Method.POST, "/open-file").body(path.normalize().toString()),
        )
    }
}
