plugins {
    id("kotlin-convention")
    id("native-image-convention")
    alias(libs.plugins.kotlinx.plugin.serialization)
}

dependencies {
    compileOnly(libs.graalvm.svm)
    api(libs.serialization.json)
    implementation("com.squareup:kotlinpoet:2.1.0")

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
