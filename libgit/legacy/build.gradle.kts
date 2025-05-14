plugins {
    id("kotlin-convention")
    id("native-image-convention")
}

dependencies {
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
