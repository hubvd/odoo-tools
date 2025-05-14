import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin-convention")
    id("native-image-convention")
}

dependencies {
    compileOnly(libs.graalvm.svm)
}

tasks.named("nativeCompile") {
    enabled = false
}

tasks.named("nativeRun") {
    enabled = false
}
