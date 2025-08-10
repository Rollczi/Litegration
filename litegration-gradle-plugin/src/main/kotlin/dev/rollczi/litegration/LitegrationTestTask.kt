package dev.rollczi.litegration

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import java.io.File
import java.nio.file.Files
import javax.inject.Inject

abstract class LitegrationTestTask @Inject constructor(
    private val fileSystemOperations: FileSystemOperations
) : Test() {

    @get:Input
    abstract val plugin: Property<File>

    @get:InputFiles
    val cachedFiles: ConfigurableFileCollection = project.objects.fileCollection()

    @get:Input
    var serverVersion: String = "1.21"

    /**
     * By changing the setting below to TRUE you are indicating your agreement to Minecraft EULA (https://aka.ms/MinecraftEULA).
     */
    @get:Input
    var eula: Boolean = false

    @TaskAction
    override fun executeTests() {
        acceptEula()
        copyPlugin()
        environment("LITEGRATION_PLUGIN", plugin.get().toPath())
        environment("LITEGRATION_SERVER_VERSION", serverVersion)
        super.executeTests()
    }

    private fun acceptEula() {
        if (eula) {
            org.gradle.internal.cc.base.logger.warn("---------------------------------------------------------------------------------------------------")
            logger.warn("EULA has been accepted by setting 'eula = true' in your gradle file.")
            logger.warn("By using this setting you are indicating your agreement to Mojang's EULA (https://account.mojang.com/documents/minecraft_eula).")
            logger.warn("If you do not agree to the above EULA please this gradle process and remove this flag immediately.")
            org.gradle.internal.cc.base.logger.warn("---------------------------------------------------------------------------------------------------")
            jvmArgs("-Dcom.mojang.eula.agree=true")
        } else {
            throw RuntimeException("Minecraft EULA not accepted. By changing the 'eula = true' in your build.gradle file, you are indicating your agreement to Minecraft EULA (https://aka.ms/MinecraftEULA).")
        }
    }

    private fun copyPlugin() {
        val pluginsDir = workingDir.resolve("plugins")
        if (!workingDir.exists()) {
            Files.createDirectories(workingDir.toPath())
        }

        if (pluginsDir.exists()) {
            deleteFolder(pluginsDir)
        }

        Files.createDirectories(pluginsDir.toPath())
        fileSystemOperations.copy { copy ->
            copy.from(plugin.get().toPath())
            copy.into(pluginsDir)
        }
    }

    fun deleteFolder(folder: File) {
        val files: Array<File>? = folder.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory()) {
                    deleteFolder(file)
                } else {
                    file.delete()
                }
            }
        }
        folder.delete()
    }

}