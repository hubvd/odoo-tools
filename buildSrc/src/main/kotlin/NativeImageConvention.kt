import org.graalvm.buildtools.gradle.dsl.GraalVMExtension
import org.graalvm.buildtools.gradle.dsl.GraalVMReachabilityMetadataRepositoryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.assign

class NativeImageConvention : Plugin<Project>{
    override fun apply(project: Project) {
        project.plugins.apply("org.graalvm.buildtools.native")

        project.tasks.getByName("build") {
            dependsOn("nativeCompile")
        }

        project.extensions.configure<GraalVMExtension>("graalvmNative") { configure(project) }
    }

    private fun GraalVMExtension.configure(project: Project) {
        (this as ExtensionAware).extensions.configure<GraalVMReachabilityMetadataRepositoryExtension>("metadataRepository") {
            uri(project.rootDir.resolve("reachability-metadata"))
            enabled = true
        }
        testSupport = true
        binaries {
            // FIXME: https://youtrack.jetbrains.com/issue/KT-60211/Kotlin-1.9.0-kotlin.annotation.AnnotationTarget-was-unintentionally-initialized-at-build-time-GraalVM-nativeTest
            // https://github.com/oracle/graal/issues/6957
            val enums = listOf(
                "kotlin.annotation.AnnotationTarget",
                "kotlin.annotation.AnnotationRetention",
            ).joinToString(",")

            named("test") {
                quickBuild = true
                buildArgs(
                    "--initialize-at-build-time=$enums",
                )
            }

            named("main") {
                buildArgs(
                    "--static",
                    "--libc=glibc",
                    "--install-exit-handlers",
                    "-march=native",
                    "--initialize-at-build-time=$enums",
                )
            }

        }
    }

}
