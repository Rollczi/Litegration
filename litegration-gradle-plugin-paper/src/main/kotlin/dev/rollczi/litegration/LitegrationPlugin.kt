package dev.rollczi.litegration

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.plugins.ide.idea.model.IdeaModel

class LitegrationPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        createSourceAndTask("paper", "testPaper", "runTestPaper")
    }

    private fun Project.createSourceAndTask(platform: String, sourcesName: String, workingDir: String) {
        val customSources = createSources(sourcesName)

        plugins.apply("idea")
        extensions.getByType(IdeaModel::class.java).module {
            it.testSources.from(customSources.java.srcDirs)
            it.testResources.from(customSources.resources.srcDirs)
        }

        val testTask = tasks.register(sourcesName, LitegrationTestTask::class.java) { task ->
            task.group = "verification"
            task.description = "Runs the $platform tests."
            task.testClassesDirs = customSources.output.classesDirs
            task.classpath = customSources.runtimeClasspath
            task.useJUnitPlatform()
            task.workingDir(workingDir)
            task.outputs.upToDateWhen { false }
        }.get()

        val litegrationVersion = "0.2.0"
        dependencies.add("${sourcesName}Implementation", "dev.rollczi:litegration-api:$litegrationVersion")
        dependencies.add("${sourcesName}Implementation", "dev.rollczi:litegration-junit-api:$litegrationVersion")
        dependencies.add("${sourcesName}Implementation", "dev.rollczi:litegration-junit-${platform}-engine:$litegrationVersion")
        dependencies.add("${sourcesName}Implementation", "org.junit.jupiter:junit-jupiter")
        dependencies.add("${sourcesName}RuntimeOnly", "org.junit.platform:junit-platform-launcher")

        afterEvaluate {
            autoDetectJar(testTask)
        }
    }

    private fun Project.createSources(sourcesName: String): SourceSet {
        val sources = extensions.getByName("sourceSets") as SourceSetContainer
        val customSources = sources.create(sourcesName) {
            it.java.srcDir("src/$sourcesName/java")
            it.resources.srcDir("src/$sourcesName/resources")
            it.compileClasspath += sources.getByName("main").output
            it.runtimeClasspath += sources.getByName("main").output
        }
        return customSources
    }

    private fun Project.autoDetectJar(testTask: LitegrationTestTask) {
        if (!testTask.plugin.isPresent) {
            val jarTask =
                if (tasks.names.contains("shadowJar")) tasks.named("shadowJar", AbstractArchiveTask::class.java)
                else tasks.named(JavaPlugin.JAR_TASK_NAME, AbstractArchiveTask::class.java)
            val jarFile = jarTask.flatMap { it.archiveFile }.get().asFile

            testTask.plugin.set(jarFile)
            testTask.dependsOn(jarTask)
        }
    }

}