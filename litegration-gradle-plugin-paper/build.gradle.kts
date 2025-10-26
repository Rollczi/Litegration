plugins {
    `litegration-java`
    `litegration-publish-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("paperIntegrationTest") {
            id = "dev.rollczi.litegration.paper"
            displayName = "Litegration Paper Gradle Plugin"
            implementationClass = "dev.rollczi.litegration.LitegrationPlugin"
        }
    }
}