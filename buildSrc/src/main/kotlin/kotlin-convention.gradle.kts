import gradle.kotlin.dsl.accessors._6da864ba4650c27f2bcad2ebfa914628.spotless
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-convention")
    kotlin("jvm")
    id("spotless")
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "19"
        javaParameters = true
        freeCompilerArgs = listOf("-Xcontext-receivers", "-opt-in=kotlin.ExperimentalStdlibApi")
    }
}

kotlin {
    sourceSets["main"].kotlin.setSrcDirs(listOf("src"))
    sourceSets["test"].kotlin.setSrcDirs(listOf("test"))
}

spotless {
    kotlin {
        ktlint("0.48.0")
    }
}
