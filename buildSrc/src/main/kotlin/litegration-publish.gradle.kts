plugins {
    id("java-library")
    id("maven-publish")
}

java {
    withSourcesJar()
    withJavadocJar()
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

    publications {
        create<MavenPublication>("maven") {
            from(project.components["java"])
        }
    }
}