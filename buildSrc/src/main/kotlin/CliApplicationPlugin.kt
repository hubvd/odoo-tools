import org.graalvm.buildtools.gradle.dsl.GraalVMExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByName

open class CliApplicationPluginExtension {
    var name: String? = "name"
    var mainClass: String? = "main"
}

class CliApplicationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("kotlin-convention")
        project.plugins.apply("org.graalvm.buildtools.native")

        project.dependencies {
            dependencies.add("implementation", "com.github.ajalt.clikt:clikt:3.5.0")
            dependencies.add("implementation", "com.github.ajalt.mordant:mordant:2.0.0-beta8")
        }

        val extension = project.extensions.create<CliApplicationPluginExtension>("cli")

        project.afterEvaluate {
            project.extensions.configure<GraalVMExtension>("graalvmNative") {
                testSupport.set(false)
                binaries {
                    named("main") {
                        javaLauncher.set(
                            project.extensions.getByName<JavaToolchainService>("javaToolchains").launcherFor {
                                languageVersion.set(JavaLanguageVersion.of(19))
                                vendor.set(JvmVendorSpec.matching("GraalVM Community"))
                            }
                        )
                    }
                }

                binaries {
                    named("main") {
                        imageName.set(extension.name!!)
                        mainClass.set(extension.mainClass!!)
                        requiredVersion.set("22.3")
                        buildArgs("--static", "--install-exit-handlers")
                    }
                }
            }
        }
    }
}
