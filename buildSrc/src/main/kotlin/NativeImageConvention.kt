import org.graalvm.buildtools.gradle.dsl.GraalVMExtension
import org.graalvm.buildtools.gradle.dsl.GraalVMReachabilityMetadataRepositoryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.assign

class NativeImageConvention : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("org.graalvm.buildtools.native")

        project.tasks.getByName("build") {
            dependsOn("nativeCompile")
        }

        project.extensions.configure<GraalVMExtension>("graalvmNative") { configure(project) }
    }

    private fun GraalVMExtension.configure(project: Project) {
        (this as ExtensionAware).extensions.configure<GraalVMReachabilityMetadataRepositoryExtension>(
            "metadataRepository",
        ) {
            uri(project.rootDir.resolve("reachability-metadata"))
            enabled = true
        }
        testSupport = true
        binaries {
            val commonFlags = arrayOf<String>()

            named("test") {
                buildArgs(
                    *commonFlags,
                )
            }

            named("main") {
                buildArgs(
                    "--install-exit-handlers",
                    "-march=native",
                    *commonFlags,
                )
            }
        }
    }
}
