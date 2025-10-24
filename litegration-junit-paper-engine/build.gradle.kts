plugins {
    `litegration-java`
    `litegration-publish`
}

dependencies {
    api(project(":litegration-api"))
    api(project(":litegration-junit-api"))
    implementation("org.junit.jupiter:junit-jupiter-api:5.13.3")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.13.3")
    compileOnly("org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT")
}