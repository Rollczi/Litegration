plugins {
    id("java-library")
}

group = "dev.rollczi"
version = "0.2.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.opencollab.dev/maven-releases")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}