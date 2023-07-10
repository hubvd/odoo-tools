import org.graalvm.buildtools.gradle.dsl.GraalVMExtension
import org.graalvm.buildtools.gradle.dsl.GraalVMReachabilityMetadataRepositoryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

interface CliApplicationPluginExtension {
    val name: Property<String>
    val mainClass: Property<String>
    val generateSubcommandBinaries: ListProperty<String>
}

class CliApplicationPlugin : Plugin<Project> {
    private lateinit var extension: CliApplicationPluginExtension

    override fun apply(project: Project) {
        project.plugins.apply("kotlin-convention")
        project.plugins.apply("org.graalvm.buildtools.native")

        project.dependencies {
            dependencies.add("implementation", "com.github.ajalt.clikt:clikt:4.0.0")
            dependencies.add("implementation", "com.github.ajalt.mordant:mordant:2.0.0")
        }

        this.extension = project.extensions.create<CliApplicationPluginExtension>("cli")

        project.tasks.withType<KotlinCompile> {
            compilerOptions {
                freeCompilerArgs.add("-opt-in=com.github.ajalt.mordant.terminal.ExperimentalTerminalApi")
            }
        }

        project.tasks.register<GenerateSubcommandBinariesTask>("generateSubcommandBinaries") {
            group = "build"
            description = "generate subcommand binaries"
            binaryName = extension.name
            generateSubcommandBinaries = extension.generateSubcommandBinaries
        }

        project.tasks.getByName("build") {
            dependsOn("nativeCompile")
            dependsOn("generateSubcommandBinaries")
        }

        project.afterEvaluate(::afterEvaluate)
    }

    private fun afterEvaluate(project: Project) {
        project.extensions.configure<GraalVMExtension>("graalvmNative") {
            (this as ExtensionAware).extensions.configure<GraalVMReachabilityMetadataRepositoryExtension>("metadataRepository") {
                uri(project.rootDir.resolve("reachability-metadata"))
                enabled = true
            }
            testSupport = false
            binaries {
                named("main") {
                    imageName = extension.name.get()
                    mainClass = extension.mainClass.get()

                    // --static -> -H:+StaticExecutableWithDynamicLibC TODO: PR for musl libc support in mordant ?
                    buildArgs(
                        "-H:+StaticExecutableWithDynamicLibC",
                        "--install-exit-handlers",
                        "-H:+PrintClassInitialization",
                        "-march=native",
                    )
                }
            }
        }
    }

}

abstract class GenerateSubcommandBinariesTask : DefaultTask() {

    @get:Input
    abstract val binaryName: Property<String>

    @get:Input
    abstract val generateSubcommandBinaries: ListProperty<String>

    private fun generateCSource(subcommand: String) = buildString {
        append("""
            #include <unistd.h>
            #include <stdio.h>
            #include <errno.h>

            int main(int argc, char *argv[]) {
                char* binaryName = "
        """.trimIndent())
        append(binaryName.get())
        appendLine("\";")
        appendLine("    char* modifiedArgv[argc + 2];")
        appendLine("    modifiedArgv[0] = binaryName;")
        append("    modifiedArgv[1] = \"")
        append(subcommand)
        appendLine("\";")
        appendLine("""
            for (int i = 2; i <= argc; i++) {
                modifiedArgv[i] = argv[i - 1];
            }
            modifiedArgv[argc + 1] = NULL;
            if (execvp(binaryName, modifiedArgv) == -1) {
                perror("Error executing binary");
                return 1;
            }
            return 0;
        }
        """.trimIndent())
    }

    @TaskAction
    fun generatesubcommandBinaries() {
        val subcommands: List<String> = generateSubcommandBinaries.orNull?.takeIf { it.isNotEmpty() } ?: return
        val subcommandDir = project.buildDir.resolve("subcommands").also { it.mkdir() }
        val generatedDir = subcommandDir.resolve("generated").also { it.mkdir() }
        val outDir = subcommandDir.resolve("out").also { it.mkdir() }
        subcommands.forEach { subcommand ->
            val sourceFile = generatedDir.resolve(subcommand + ".c")
            val outFile = outDir.resolve(subcommand)
            sourceFile.writeText(generateCSource(subcommand))
            ProcessBuilder(
                "cc",
                "-Os",
                "-s",
                sourceFile.toString(),
                "-o",
                outFile.toString()
            ).start().waitFor()
        }
    }
}
