package dev.rollczi.litegration

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.plugins.ide.idea.model.IdeaModel
import java.util.Locale
import java.util.Locale.getDefault

class LitegrationPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        createSourceAndTask("paper", "testPaper")
    }

    private fun Project.createSourceAndTask(platform: String, sourcesName: String) {
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
            task.description = "Runs the $platform tests."
            task.testClassesDirs = customSources.output.classesDirs
            task.classpath = customSources.runtimeClasspath
            task.useJUnitPlatform()
            task.workingDir(sourcesName)
            task.outputs.upToDateWhen { false }
        }

        val litegrationVersion = "0.1.0"
        dependencies.add("${sourcesName}Implementation", "dev.rollczi:litegration-api:$litegrationVersion")
        dependencies.add("${sourcesName}Implementation", "dev.rollczi:litegration-junit-api:$litegrationVersion")
        dependencies.add("${sourcesName}Implementation", "dev.rollczi:litegration-junit-${platform}-engine:$litegrationVersion")
        dependencies.add("${sourcesName}RuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

}