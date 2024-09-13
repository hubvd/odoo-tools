package com.github.hubvd.odootools.pycharmctl.plugin

import com.github.hubvd.odootools.pycharmctl.api.PycharmCtl
import com.github.hubvd.odootools.pycharmctl.api.PycharmProject
import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import kotlin.io.path.Path

object PycharmCtlService {
    private val logger = Logger.getInstance(PycharmCtlService::class.java)
    private val pycharmCtl: PycharmCtl = PycharmCtlImpl()

    private val routes = routes(
        "/project" bind Method.GET to { project() },
        "/projects" bind Method.GET to { projects() },
        "/focus" bind Method.POST to ::focus,
        "/open" bind Method.POST to ::open,
        "/open-file" bind Method.POST to ::openFile,
    )

    private val json = Json

    private fun project(): Response = Response(
        Status.OK,
    ).body(Json.encodeToString(PycharmProject.PycharmProjectInfo.serializer(), pycharmCtl.currentProject()))

    private fun projects(): Response = Response(Status.OK).body(
        Json.encodeToString(
            ListSerializer(PycharmProject.PycharmProjectInfo.serializer()),
            pycharmCtl.openedProjects(),
        ),
    )

    private fun focus(request: Request): Response {
        val project = json.decodeFromString(PycharmProject.serializer(), request.bodyString())
        pycharmCtl.focus(project)
        return Response(Status.OK)
    }

    private fun open(request: Request): Response {
        val newWindow = request.query("new") == "true"
        val project = json.decodeFromString(PycharmProject.PycharmProjectByPath.serializer(), request.bodyString())
        if (newWindow) {
            pycharmCtl.openProjectInNewWindow(project)
        } else {
            pycharmCtl.openProjectInCurrentWindow(project)
        }
        return Response(Status.OK)
    }

    private fun openFile(request: Request): Response {
        val path = Path(request.bodyString())
        pycharmCtl.openFile(path)
        return Response(Status.OK)
    }

    init {

        Filter { next ->
            { request ->
                try {
                    next(request)
                } catch (e: Exception) {
                    Response(Status.INTERNAL_SERVER_ERROR).body(e.stackTraceToString())
                }
            }
        }
            .then(routes)
            .asServer(SunHttp(8000)).start()
    }
}
