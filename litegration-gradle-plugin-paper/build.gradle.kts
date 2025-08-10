plugins {
    `litegration-publish-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("paperIntegrationTest") {
            id = "dev.rollczi.litegration.paper"
            implementationClass = "dev.rollczi.litegration.LitegrationPlugin"
        }
    }
}