plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "dev.rollczi"
version = "0.1.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
    mavenLocal()
}


publishing {
    repositories {
        maven {
            name = "CompanyRepo"
            url = uri(System.getenv("MY_MAVEN_URL") ?: "https://my.repo/repository/maven-releases/")
            credentials {
                username = findProperty("repoUser") as String? ?: System.getenv("REPO_USER")
                password = findProperty("repoPassword") as String? ?: System.getenv("REPO_PASSWORD")
            }
        }
    }
}