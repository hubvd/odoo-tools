plugins {
    id("cli-application")
    id("launcher-checksums")
    alias(libs.plugins.kotlinx.plugin.serialization)
}

cli {
    name = "odoo"
    mainClass = "com.github.hubvd.odootools.odoo.OdooKt"
}

dependencies {
    implementation(project(":config"))
    implementation(project(":workspace"))
    implementation(libs.coroutines.core)
    implementation(libs.process)
    implementation(libs.xmlbuilder)
    implementation(libs.kodein.di)
    implementation(libs.kotlin.reflect)
    implementation(libs.serialization.json)

    testImplementation(testFixtures(project(":workspace")))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertk)
}

val blackVersion = project.findProperty("black.version") as String

spotless {
    python {
        target("resources/launcher/**/*.py")
        black(blackVersion)
    }
    json {
        target("resources/META-INF/native-image/**/*.json")
        gson()
            .indentWithSpaces(4)
            .sortByKeys()
    }
}
