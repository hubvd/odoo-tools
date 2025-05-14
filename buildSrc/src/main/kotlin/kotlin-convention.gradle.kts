import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
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
    compilerOptions {
        jvmTarget = JvmTarget.JVM_22
        javaParameters = true
        freeCompilerArgs =
            listOf(
                "-Xcontext-receivers",
                "-opt-in=kotlin.ExperimentalStdlibApi",
            )
        languageVersion = KotlinVersion.KOTLIN_2_0
    }
}

kotlin {
    sourceSets["main"].kotlin.setSrcDirs(listOf("src"))
    sourceSets["test"].kotlin.setSrcDirs(listOf("test"))

    // Needed since kotlin 1.9 ??
    sourceSets["main"].resources.setSrcDirs(listOf("resources"))
    sourceSets["test"].resources.setSrcDirs(listOf("test-resources"))
}

spotless {
    kotlin {
        ktlint("1.5.0")
            .setEditorConfigPath("${project.rootDir}/.editorconfig")
    }
}
