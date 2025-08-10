package dev.rollczi.litegration

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.plugins.ide.idea.model.IdeaModel

class LitegrationPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        createSourceAndTask("testPaper")
    }

    private fun Project.createSourceAndTask(sourcesName: String) {
        val sources = extensions.getByName("sourceSets") as SourceSetContainer
        val customSources = sources.create(sourcesName) {
            it.java.srcDir("src/$sourcesName/java")
            it.resources.srcDir("src/$sourcesName/resources")
            it.compileClasspath += sources.getByName("main").output
            it.runtimeClasspath += sources.getByName("main").output
        }

        extensions.getByType(IdeaModel::class.java).module {
            it.testSources.from(customSources.java.srcDirs)
            it.testResources.from(customSources.resources.srcDirs)
        }

        tasks.register(sourcesName, LitegrationTestTask::class.java) { task ->
            task.group = "verification"
            task.description = "Runs the $sourcesName tests."
            task.testClassesDirs = customSources.output.classesDirs
            task.classpath = customSources.runtimeClasspath
            task.useJUnitPlatform()
            task.workingDir(sourcesName)
            task.outputs.upToDateWhen { false }
        }

        dependencies.add("${sourcesName}Implementation", "dev.rollczi:litegration-junit-paper-api:0.1.0")
        dependencies.add("${sourcesName}Implementation", "dev.rollczi:litegration-junit-paper-engine:0.1.0")
        dependencies.add("${sourcesName}RuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

}