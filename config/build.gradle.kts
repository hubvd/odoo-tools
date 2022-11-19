plugins {
    id("kotlin-convention")
    kotlin("plugin.serialization") version "1.7.20"
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.4.1")
    implementation("com.akuleshov7:ktoml-core:0.3.0")
    implementation("org.slf4j:slf4j-nop:2.0.3")
}
