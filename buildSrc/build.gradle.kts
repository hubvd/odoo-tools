plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0-RC")
    // implementation("org.jlleitschuh.gradle:ktlint-gradle:11.0.0")
    implementation("org.graalvm.buildtools.native:org.graalvm.buildtools.native.gradle.plugin:0.9.19")
    implementation("dev.olshevski:gradle-versions-plugin:1.0.1")
}
