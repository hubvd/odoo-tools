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
    implementation("org.redundent:kotlin-xml-builder:1.8.0")
    implementation("org.kodein.di:kodein-di:7.16.0")
    implementation("com.github.pgreze:kotlin-process:1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}
