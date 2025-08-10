plugins {
    `litegration-java`
    `litegration-publish`
}

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    api(project(":litegration-junit-paper-api"))
    implementation("org.junit.jupiter:junit-jupiter-api:5.13.3")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.13.3")
    compileOnly("org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT")
}