import java.util.zip.Adler32

plugins {
    id("cli-application")
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
}

sourceSets["main"].resources.srcDir(buildDir.resolve("generated-resources"))

tasks.register("generateLauncherChecksums") {
    val path = sourceSets["main"].resources.srcDirs.first().resolve("launcher")
    dependsOn(sourceSets["main"].resources)
    doLast {
        val adler32 = Adler32()
        val checkSums = StringBuilder()
        path.walkTopDown().filter { it.isFile && it.name.endsWith(".py") }.forEach { file ->
            val bytes = file.readBytes()
            adler32.reset()
            adler32.update(bytes)

            checkSums.append(adler32.value)
            checkSums.append(':')
            checkSums.append(file.relativeTo(path))
            checkSums.appendLine()
        }

        buildDir.resolve("generated-resources/launcher")
            .apply { mkdirs() }
            .resolve("checksums")
            .writeText(checkSums.toString())
    }
}

tasks.getByName("processResources").dependsOn("generateLauncherChecksums")
