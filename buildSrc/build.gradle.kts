plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
    implementation("org.graalvm.buildtools.native:org.graalvm.buildtools.native.gradle.plugin:0.9.20")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.13.0")
}
