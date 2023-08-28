plugins {
    id("kotlin-convention")
    alias(libs.plugins.kotlinx.plugin.serialization)
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":config"))
    implementation(libs.kodein.di)
    implementation(libs.serialization.json)
}

sourceSets["testFixtures"].resources.setSrcDirs(listOf("test-fixtures-resources"))
sourceSets["testFixtures"].java.setSrcDirs(emptyList<String>())

kotlin {
    sourceSets["testFixtures"].kotlin.setSrcDirs(listOf("test-fixtures"))
    sourceSets["testFixtures"].resources.setSrcDirs(listOf("test-fixtures-resources"))
}
