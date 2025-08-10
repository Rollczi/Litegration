plugins {
    `litegration-java`
    `litegration-publish`
}

dependencies {
    api(project(":litegration-api"))
    api(project(":litegration-client-api"))

    api("org.geysermc.mcprotocollib:protocol:1.21.7-1")
}