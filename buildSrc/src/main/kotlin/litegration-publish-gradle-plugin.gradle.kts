plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "dev.rollczi"
version = "0.1.0"

repositories {
    mavenCentral()
}

publishing {
    repositories {
        val isSnapshot = version.toString().endsWith("-SNAPSHOT")
        maven {
            url = if (isSnapshot) uri("https://repo.eternalcode.pl/snapshots")
                else uri("https://repo.eternalcode.pl/releases")

            credentials {
                username = System.getenv("ETERNAL_CODE_MAVEN_USERNAME")
                password = System.getenv("ETERNAL_CODE_MAVEN_PASSWORD")
            }
        }
    }
}