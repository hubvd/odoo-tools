package com.github.hubvd.odootools.odoo

import com.github.hubvd.odootools.workspace.Workspace
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.name

data class TestTag(val module: String?, val clazz: String?, val method: String?) {
    companion object {
        private val TAG_RE = "(?:\\w+)?(?:/(?<module>\\w+))?(?::(?<class>\\w+))?(?:\\.(?<method>\\w+))?".toRegex()

        operator fun invoke(tag: String): TestTag {
            val match = TAG_RE.matchEntire(tag)!!
            return TestTag(
                match.groups["module"]?.value,
                match.groups["class"]?.value,
                match.groups["method"]?.value,
            )
        }
    }
}

fun TestTag.toAddons(workspace: Workspace, addonsPath: String?): List<String> {
    if (module != null) return listOf(module)
    val rgArg = mutableListOf("-g", "**/tests/**/test_*.py", "--no-config", "-l")
    when {
        clazz != null && method != null -> {
            rgArg += "--multiline"
            rgArg += "--multiline-dotall"
            rgArg += "$clazz.*$method"
        }

        clazz != null -> {
            rgArg += """class\s+$clazz"""
        }

        method != null -> {
            rgArg += """def\s+$method\("""
        }

        else -> {
            return emptyList()
        }
    }
    if (addonsPath != null) {
        addonsPath.splitToSequence(',')
            .map { value ->
                Path(
                    when {
                        value.startsWith("~/") -> value.replaceRange(0..1, System.getProperty("user.home") + "/")
                        value == "~" -> System.getProperty("user.home")
                        else -> value
                    },
                )
            }
            .map { path -> if (path.isAbsolute) path else workspace.path / path }
            .forEach { rgArg += it.toString() }
    } else {
        rgArg += workspace.path.toString()
    }
    val matches = runBlocking {
        process("rg", *rgArg.toTypedArray(), stdout = Redirect.CAPTURE, stderr = Redirect.SILENT).output
    }
    return matches.filter { it.isNotBlank() }.map { line -> Path(line).parent.parent.name }
}
