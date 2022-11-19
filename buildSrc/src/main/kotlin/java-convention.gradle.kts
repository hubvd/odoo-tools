plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    targetCompatibility = JavaVersion.toVersion(17)
    sourceCompatibility = JavaVersion.toVersion(17)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

sourceSets["main"].resources.setSrcDirs(listOf("resources"))
sourceSets["main"].java.setSrcDirs(emptyList<String>())
sourceSets["test"].resources.setSrcDirs(listOf("test-resources"))
sourceSets["test"].java.setSrcDirs(emptyList<String>())

