plugins {
    id("cli-application")
    alias(libs.plugins.kotlinx.plugin.serialization)
}

cli {
    name = "actions"
    mainClass = "com.github.hubvd.odootools.actions.ActionsKt"
}

dependencies {
    implementation(project(":config"))
    implementation(project(":workspace"))

    implementation(libs.serialization.json)
    implementation(libs.coroutines.core)
    implementation(libs.process)
    implementation(libs.kodein.di)
    implementation(libs.http4k.core)
}
