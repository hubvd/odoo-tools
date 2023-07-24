package com.github.hubvd.odootools.worktree

import com.github.hubvd.odootools.workspace.Workspace
import org.redundent.kotlin.xml.Node
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml
import java.nio.file.FileAlreadyExistsException
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText

context(Node)
private infix fun String.eq(value: Any) {
    "option" {
        attribute("name", this@eq)
        attribute("value", value)
    }
}

class Pycharm(private val workspace: Workspace, private val repositories: List<Repository>) {
    private fun generateIml() = xml("module") {
        includeXmlProlog = true
        attribute("type", "PYTHON_MODULE")
        attribute("version", "4")
        "component"("name" to "NewModuleRootManager") {
            "content"("url" to "file://\$MODULE_DIR\$") {
                repositories.forEach { repo ->
                    "sourceFolder"(
                        "url" to "file://\$MODULE_DIR\$/${repo.pathName}",
                        "isTestSource" to "false",
                    )
                }
                "excludeFolder"("url" to "file://\$MODULE_DIR\$/venv")
            }
        }
    }

    private fun generateRemoteDebuggingConfig() = xml("component") {
        attribute("name", "ProjectRunConfigurationManager")
        "configuration"(
            "default" to "false",
            "name" to "remote debugger",
            "type" to "PyRemoteDebugConfigurationType",
            "factoryName" to "Python Remote Debug",
        ) {
            "module"("name" to workspace.name)
            "PORT" eq 10000
            "HOST" eq "localhost"
            "PathMappingSettings" {
                "option"("name" to "pathMappings") {
                    "list"()
                }
            }
            "REDIRECT_OUTPUT" eq false
            "SUSPEND_AFTER_CONNECT" eq false
            "method"("v" to "2")
        }
    }

    private fun generateVcs() = xml("project") {
        includeXmlProlog = true
        attribute("version", "4")
        "component"("name" to "IssueNavigationConfiguration") {
            "option"("name" to "links") {
                "list" {
                    "IssueNavigationLink" {
                        "option"("name" to "issueRegexp", "value" to "(?i)(opw|task)[\\W\\-]*(?:id)?[\\W-]*(\\d+)")
                        "option"(
                            "name" to "linkRegexp",
                            "value" to "https://www.odoo.com/web#view_type=form&amp;model=project.task&amp;id=\$2",
                        )
                    }
                    "IssueNavigationLink" {
                        "option"("name" to "issueRegexp", "value" to "odoo/(.*)#(\\d+)")
                        "option"("name" to "linkRegexp", "value" to "https://github.com/odoo/\$1/pull/\$2")
                    }
                }
            }
        }
        "component"("name" to "VcsDirectoryMappings") {
            repositories.forEach { repo ->
                "mapping"("directory" to "\$PROJECT_DIR\$/${repo.pathName}", "vcs" to "Git")
            }
        }
    }

    private fun generateModules() = xml("project") {
        includeXmlProlog = true
        attribute("version", "4")
        "component"("name" to "ProjectModuleManager") {
            "modules" {
                "module"(
                    "fileurl" to "file://\$PROJECT_DIR\$/.idea/${workspace.name}.iml",
                    "filepath" to "file://\$PROJECT_DIR\$/.idea/${workspace.name}.iml",
                )
            }
        }
    }

    private fun generateCodeStyleConfig() = xml("component") {
        attribute("name", "VcsDirectoryMappings")
        "state" {
            "USE_PER_PROJECT_SETTINGS" eq true
        }
    }

    private fun generateProjectCodeStyle() = xml("component") {
        attribute("name", "ProjectCodeStyleConfiguration")
        "code_scheme"("name" to "Project", "version" to "173") {
            "JSCodeStyleSettings"("version" to "0") {
                "FORCE_SEMICOLON_STYLE" eq true
                "SPACES_WITHIN_OBJECT_LITERAL_BRACES" eq true
                "SPACES_WITHIN_IMPORTS" eq true
            }
        }
    }

    private fun generateScope(name: String, pattern: String) = xml("component") {
        attribute("name", "DependencyValidationManager")
        "scope"("name" to name, "pattern" to pattern)
    }

    // TODO: read this from config ?
    private fun generateScopes() = listOf(
        "web" to "file:odoo/addons/web/static/src//*.js&&!file:odoo/addons/web/static/src/legacy//*",
        "web legacy" to "file:odoo/addons/web/static/src/legacy//*.js",
        "models" to "file:*/models/*.py",
        "views" to "file:*/views/*.xml",
    ).map { it.first to { generateScope(it.first, it.second) } }

    fun saveFiles() {
        val generators = sequence {
            yield("${workspace.name}.iml" to { generateIml() })
            yield("runConfigurations/remote_debugger.xml" to { generateRemoteDebuggingConfig() })
            yield("vcs.xml" to { generateVcs() })
            yield("modules.xml" to { generateModules() })
            yield("codeStyles/codeStyleConfig.xml" to { generateCodeStyleConfig() })
            yield("codeStyles/Project.xml" to { generateProjectCodeStyle() })
            val invalidChars = Regex("[^a-zA-Z0-9.\\-_]")
            generateScopes().forEach { (name, xml) ->
                yield("scopes/${invalidChars.replace(name, "_")}.xml" to xml)
            }
        }.map { workspace.path / ".idea" / it.first to it.second }.toList()
        generators.map { it.first.parent }.toHashSet().forEach { it.createDirectories() }
        generators.forEach { (path, generator) ->
            try {
                path.writeText(
                    generator().toString(PrintOptions(indent = "  ")),
                    options = arrayOf(StandardOpenOption.CREATE_NEW),
                )
            } catch (_: FileAlreadyExistsException) {}
        }
    }
}
