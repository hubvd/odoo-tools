plugins {
    id("cli-application")
    kotlin("plugin.serialization") version "1.8.0-Beta"
}

cli {
    name = "worktree"
    mainClass = "com.github.hubvd.odootools.worktree.WorktreeKt"
}

dependencies {
    implementation(project(":config"))
    implementation(project(":workspace"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.github.pgreze:kotlin-process:1.4")
    implementation("org.kodein.di:kodein-di:7.16.0")
    implementation("org.redundent:kotlin-xml-builder:1.8.0")
}
