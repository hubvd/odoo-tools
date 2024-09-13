import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin-convention")
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

java {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        javaParameters = true
    }
}

dependencies {
    implementation(libs.http4k.core)
    implementation(project(":pycharmctl:api"))
    intellijPlatform {
        instrumentationTools()
        intellijIdeaCommunity("2024.2.0.1")
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "pycharmctl"
        version = "2024.2"
    }
}

tasks {
    runIde {
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf("-Dawt.toolkit.name=WLToolkit")
        }
    }
}
