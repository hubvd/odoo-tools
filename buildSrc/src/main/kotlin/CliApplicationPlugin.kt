import org.graalvm.buildtools.gradle.dsl.GraalVMExtension
import org.graalvm.buildtools.gradle.dsl.GraalVMReachabilityMetadataRepositoryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

open class CliApplicationPluginExtension {
    var name: String? = "name"
    var mainClass: String? = "main"
}

class CliApplicationPlugin : Plugin<Project> {
    private lateinit var extension: CliApplicationPluginExtension

    override fun apply(project: Project) {
        project.plugins.apply("kotlin-convention")
        project.plugins.apply("org.graalvm.buildtools.native")

        project.dependencies {
            dependencies.add("implementation", "com.github.ajalt.clikt:clikt:3.5.2")
            dependencies.add("implementation", "com.github.ajalt.mordant:mordant:2.0.0-beta13")
        }

        this.extension = project.extensions.create<CliApplicationPluginExtension>("cli")

        project.tasks.withType<KotlinCompile> {
            compilerOptions {
                freeCompilerArgs.add("-opt-in=com.github.ajalt.mordant.terminal.ExperimentalTerminalApi")
            }
        }

        project.tasks.getByName("build") {
            dependsOn("nativeCompile")
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
                    imageName = extension.name
                    mainClass = extension.mainClass

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
