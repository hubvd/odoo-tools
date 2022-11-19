plugins {
    id("kotlin-convention")
    kotlin("plugin.serialization") version "1.7.20"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation(project(":config"))
    implementation("org.kodein.di:kodein-di:7.16.0")
}
