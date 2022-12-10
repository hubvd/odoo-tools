plugins {
    id("kotlin-convention")
    @Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
    alias(libs.plugins.kotlinx.plugin.serialization)
}

dependencies {
    implementation(project(":config"))
    implementation(libs.kodein.di)
    implementation(libs.serialization.json)
}
