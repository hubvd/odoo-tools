plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(20)
        vendor = JvmVendorSpec.GRAAL_VM
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

sourceSets["main"].resources.setSrcDirs(listOf("resources"))
sourceSets["main"].java.setSrcDirs(emptyList<String>())
sourceSets["test"].resources.setSrcDirs(listOf("test-resources"))
sourceSets["test"].java.setSrcDirs(emptyList<String>())
