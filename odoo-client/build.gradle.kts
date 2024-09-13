import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin-convention")
    id("native-image-convention")
    alias(libs.plugins.kotlinx.plugin.serialization)
}

dependencies {
    api(libs.serialization.json)
    api(libs.http4k.core)
    api(libs.kodein.di)
    implementation(project(":config"))

    compileOnly(libs.graalvm.svm)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertk)
}

tasks.named("nativeCompile") {
    enabled = false
}

tasks.named("nativeRun") {
    enabled = false
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
    }
}
