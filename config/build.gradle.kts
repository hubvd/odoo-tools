plugins {
    id("kotlin-convention")
    @Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
    alias(libs.plugins.kotlinx.plugin.serialization)
}

dependencies {
    api(libs.serialization.core)
    implementation(libs.serialization.toml)
}
