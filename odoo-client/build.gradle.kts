import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin-convention")
    id("native-image-convention")
    alias(libs.plugins.kotlinx.plugin.serialization)
}

dependencies {
    api(libs.serialization.json)
    api(libs.http4k.core)

    compileOnly(libs.graalvm.svm)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertk)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
    }
}
