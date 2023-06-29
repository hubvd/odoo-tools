plugins {
    id("cli-application")
    alias(libs.plugins.kotlinx.plugin.serialization)
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

cli {
    name = "worktree"
    mainClass = "com.github.hubvd.odootools.worktree.WorktreeKt"
}

dependencies {
    implementation(project(":config"))
    implementation(project(":workspace"))
    implementation(libs.serialization.core)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.core)
    implementation(libs.process)
    implementation(libs.xmlbuilder)
    implementation(libs.kodein.di)
}
