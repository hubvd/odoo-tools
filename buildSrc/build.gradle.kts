plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0-RC")
    implementation("org.graalvm.buildtools.native:org.graalvm.buildtools.native.gradle.plugin:0.9.19")
    implementation("dev.olshevski:gradle-versions-plugin:1.0.1")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.12.1")
}
