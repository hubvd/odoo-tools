package com.github.hubvd.odootools.workspace

import com.github.hubvd.odootools.config.Config
import com.github.hubvd.odootools.config.ShellPath
import com.github.hubvd.odootools.workspace.WorkspaceFormat.*
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton
import kotlin.io.path.div
import kotlin.io.path.readText
import kotlinx.serialization.json.Json as Jsonx

@Serializable
data class WorkspaceConfig(val root: ShellPath, val default: String)

@Serializable
data class Workspace(val name: String, val path: ShellPath) {

    val version: Float
    val base: String

    init {
        val release = readRelease()
        version = release.first
        base = release.second
    }

    private fun readRelease(): Pair<Float, String> {
        val txt = (path / "odoo/odoo/release.py").readText()
        val (major, minor, level) = VERSION_RE.find(txt)!!.destructured
        val majorNumber = major.removePrefix("saas~")
        val version = "$majorNumber.$minor".toFloat()
        val base = if (level != "FINAL") {
            "master"
        } else {
            "${major.replace('~', '-')}.$minor"
        }
        return version to base
    }

    override fun toString(): String {
        return "Workspace(name=$name, path=$path, version=$version, base=$base)"
    }

    companion object {
        private val VERSION_RE =
            """version_info = ?\(['"]?((?:saas~)?\d+)['"]?,\s*(\d+),\s*\d+,\s*([A-Z]+)""".toRegex()
    }
}

enum class WorkspaceFormat { Name, Version, Path, Base, Json, Fish }

fun Workspace.format(format: WorkspaceFormat): String = when (format) {
    Name -> name
    Version -> version.toString()
    Path -> path.toString()
    Base -> base
    Json -> Jsonx.encodeToString(Workspace.serializer(), this)
    Fish -> buildString {
        for ((name, value) in arrayOf(
            "version" to version,
            "name" to name,
            "path" to path,
            "base" to base,
        )) {
            append("set workspace_")
            append(name)
            append(' ')
            append(value)
            append(';')
        }
    }
}

val WORKSPACE_MODULE = DI.Module("workspace") {
    bind { singleton { Config.get("workspace", WorkspaceConfig.serializer()) } }
}
