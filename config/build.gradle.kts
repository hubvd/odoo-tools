plugins {
    id("kotlin-convention")
    alias(libs.plugins.kotlinx.plugin.serialization)
}

dependencies {
    api(libs.serialization.core)
    implementation(libs.serialization.toml)
    api(libs.kodein.di)
}
