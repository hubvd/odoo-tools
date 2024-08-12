import org.graalvm.buildtools.gradle.dsl.GraalVMExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.*
import java.nio.file.Files

interface CliApplicationPluginExtension {
    val name: Property<String>
    val mainClass: Property<String>
    val generateSubcommandBinaries: ListProperty<String>
}

class CliApplicationPlugin : Plugin<Project> {
    private lateinit var extension: CliApplicationPluginExtension

    override fun apply(project: Project) {
        project.plugins.apply("kotlin-convention")
        project.plugins.apply("native-image-convention")

        val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
        project.dependencies {
            dependencies.add("implementation", libs.findLibrary("clikt").get())
            dependencies.add("implementation", libs.findLibrary("mordant").get())
        }

        this.extension = project.extensions.create<CliApplicationPluginExtension>("cli")

        project.tasks.register<GenerateSubcommandBinariesTask>("generateSubcommandBinaries") {
            group = "build"
            description = "generate subcommand binaries"
            binaryName = extension.name
            generateSubcommandBinaries = extension.generateSubcommandBinaries
        }

        project.tasks.getByName("build") {
            dependsOn("generateSubcommandBinaries")
        }

        project.afterEvaluate(::afterEvaluate)
    }

    private fun afterEvaluate(project: Project) {
        project.extensions.configure<GraalVMExtension>("graalvmNative") {
            binaries {
                named("main") {
                    imageName = extension.name.get()
                    mainClass = extension.mainClass.get()
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

    @TaskAction
    fun generateSubcommandBinaries() {
        val subcommands: List<String> = generateSubcommandBinaries.orNull?.takeIf { it.isNotEmpty() } ?: return
        val buildDir = project.layout.buildDirectory.asFile.get()
        val targetBinary = buildDir.resolve("native").resolve("nativeCompile").resolve(binaryName.get()).toPath()
        val subcommandDir = buildDir.resolve("subcommands").also { it.mkdir() }
        val outDir = subcommandDir.resolve("out").also { it.mkdir() }
        subcommands.forEach { subcommand ->
            val outFile = outDir.resolve(subcommand).toPath()
            Files.deleteIfExists(outFile)
            Files.createSymbolicLink(outFile, targetBinary)
        }
    }
}
