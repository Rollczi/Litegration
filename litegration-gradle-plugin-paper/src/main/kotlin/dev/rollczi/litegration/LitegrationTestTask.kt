package dev.rollczi.litegration

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import java.io.File

abstract class LitegrationTestTask : Test() {

    /**
     * The plugin file that will be tested (tests will be loaded by PluginClassLoader of this plugin)
     */
    @get:InputFile
    @get:Classpath
    @get:Optional
    abstract val plugin: Property<File>

    /**
     * Additional plugins that will be loaded into the server alongside the tested plugin
     */
    @get:InputFiles
    @get:Classpath
    abstract val externalPlugins: ListProperty<File>

    /**
     * Server properties that will be applied to the Minecraft server (server.properties file)
     */
    @get:Input
    abstract val serverProperties: MapProperty<String, String>

    @get:Input
    var serverVersion: String = "1.21.7"

    /**
     * By changing the setting below to TRUE, you are indicating your agreement to Minecraft EULA (https://aka.ms/MinecraftEULA).
     */
    @get:Input
    var eula: Boolean = false

    fun ignoreUnsupportedJvm() {
        systemProperty("Paper.IgnoreJavaVersion", true)
    }

    fun disablePluginRemapping() {
        systemProperty("paper.disablePluginRemapping", true)
    }

    @TaskAction
    override fun executeTests() {
        acceptEula()
        systemProperty("disable.watchdog", true)
        environment("LITEGRATION_PLUGIN", plugin.get().toPath())
        environment("LITEGRATION_EXTERNAL_PLUGINS", externalPlugins.get().joinToString("\n") { it.toPath().toString() })
        environment("LITEGRATION_SERVER_VERSION", serverVersion)
        environment("LITEGRATION_SERVER_PROPERTIES", serverProperties.get().entries.joinToString("\n") { "${it.key}=${it.value}" })
        super.executeTests()
    }

    private fun acceptEula() {
        if (!eula) {
            throw RuntimeException("Minecraft EULA not accepted. By changing the 'eula = true' in your build.gradle file, you are indicating your agreement to Minecraft EULA (https://aka.ms/MinecraftEULA).")
        }

        logger.warn("---------------------------------------------------------------------------------------------------")
        logger.warn("EULA has been accepted by setting 'eula = true' in your gradle file.")
        logger.warn("By using this setting you are indicating your agreement to Mojang's EULA (https://account.mojang.com/documents/minecraft_eula).")
        logger.warn("If you do not agree to the above EULA please this gradle process and remove this flag immediately.")
        logger.warn("---------------------------------------------------------------------------------------------------")
        jvmArgs("-Dcom.mojang.eula.agree=true")
    }

}