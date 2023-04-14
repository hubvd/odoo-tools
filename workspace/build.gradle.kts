plugins {
    id("kotlin-convention")
    alias(libs.plugins.kotlinx.plugin.serialization)
}

dependencies {
    implementation(project(":config"))
    implementation(libs.kodein.di)
    implementation(libs.serialization.json)
}
