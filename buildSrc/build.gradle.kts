plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.plugin)
    implementation(libs.graalvm.native.buildtools)
    implementation(libs.spotless)
}
