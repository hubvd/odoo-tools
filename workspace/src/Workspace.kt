package com.github.hubvd.odootools.workspace

import com.github.hubvd.odootools.config.Config
import com.github.hubvd.odootools.config.ShellPath
import com.github.hubvd.odootools.workspace.WorkspaceFormat.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.readText
import java.nio.file.Path as NioPath

@Serializable
data class WorkspaceConfig(
    val root: ShellPath,
    val default: String,
    val odooToolsPath: ShellPath = Path(System.getProperty("user.home"), "odoo-tools"),
)

interface Workspace {
    val name: String
    val path: NioPath
    val version: Float
    val base: String

    companion object {
        operator fun invoke(name: String, path: NioPath): Workspace = WorkspaceImpl(name, path)
    }
}

internal data class WorkspaceImpl(override val name: String, override val path: ShellPath) : Workspace {

    override val version: Float
    override val base: String
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
    Json -> buildJsonObject {
        put("version", version)
        put("name", name)
        put("path", path.toString())
        put("base", base)
    }.toString()
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
    bind { singleton { instance<Config>().get("workspace", WorkspaceConfig.serializer()) } }
}
