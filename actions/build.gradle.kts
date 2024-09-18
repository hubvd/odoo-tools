import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("cli-application")
    alias(libs.plugins.kotlinx.plugin.serialization)
}

cli {
    name = "actions"
    mainClass = "com.github.hubvd.odootools.actions.ActionsKt"
    generateSubcommandBinaries = listOf(
        "odooctl",
        "pycharm",
        "restore",
        "qr",
        "checkout",
        "new",
        "pr",
        "bisect",
        "opener",
        "db",
        "repo",
        "is-off",
        "who-is",
    )
}

dependencies {
    implementation(project(":config"))
    implementation(project(":workspace"))
    implementation(project(":odoo-client"))
    implementation(project(":pycharmctl:client"))

    implementation(libs.serialization.json)
    implementation(libs.coroutines.core)
    implementation(libs.process)
    implementation(libs.kodein.di)
    implementation(libs.http4k.core)
    implementation(libs.arrow.core)

    compileOnly(libs.graalvm.svm)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertk)

    testImplementation(testFixtures(project(":workspace")))
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
    }
}
