import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import java.io.File
import java.util.zip.Adler32

class LauncherChecksumPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val sourceSets = target.extensions.getByType(JavaPluginExtension::class.java).sourceSets
        val resources = sourceSets["main"].resources
        val buildDir = target.layout.buildDirectory.asFile.get()
        val generatedResourcesDir = buildDir.resolve("generated-resources")

        target.tasks.register<LauncherChecksumTask>("generateLauncherChecksums") {
            this.group = "launcher"
            this.description = "generate launcher checksums"
            this.resources.set(resources)
            this.generatedResourcesDir.set(generatedResourcesDir)
            this.dependsOn(resources)
        }

        resources.srcDir(generatedResourcesDir)
        target.tasks.getByName("processResources").dependsOn("generateLauncherChecksums")
    }
}

abstract class LauncherChecksumTask : DefaultTask() {

    @get:Input
    abstract val resources: Property<SourceDirectorySet>

    @get:Input
    abstract val generatedResourcesDir: Property<File>

    @TaskAction
    fun generateChecksums() {
        val launcherPath = resources.get().srcDirs.first().resolve("launcher")
        if (!launcherPath.isDirectory) throw GradleException("Couldn't find the launcher directory")

        val adler32 = Adler32()
        val checkSums = StringBuilder()

        launcherPath.walkTopDown().filter { it.isFile && it.name.endsWith(".py") }.forEach { file ->
            val bytes = file.readBytes()
            adler32.reset()
            adler32.update(bytes)

            checkSums.append(adler32.value)
            checkSums.append(':')
            checkSums.append(file.relativeTo(launcherPath))
            checkSums.appendLine()
        }

        generatedResourcesDir.get().resolve("launcher")
            .apply { mkdirs() }
            .resolve("checksums")
            .writeText(checkSums.toString())
    }
}
