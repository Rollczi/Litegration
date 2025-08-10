package dev.rollczi.litegration.paper.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ServerConfigurer {

    private final Path serverJar;
    private final List<Path> pluginJars = new ArrayList<>();
    private final Map<String, String> serverProperties = new HashMap<>();

    public ServerConfigurer(Path serverJar) {
        this.serverJar = serverJar;
    }

    public ServerConfigurer withPlugin(Path pluginJar) {
        this.pluginJars.add(pluginJar);
        return this;
    }

    public ServerConfigurer withPlugins(List<Path> paths) {
        this.pluginJars.addAll(paths);
        return this;
    }

    public ServerConfigurer withServerProperty(String key, String value) {
        this.serverProperties.put(key, value);
        return this;
    }

    public ServerConfigurer withServerProperties(Map<String, String> properties) {
        this.serverProperties.putAll(properties);
        return this;
    }

    public ServerInstance start() {
        this.initializeServerProperties();

        for (Path pluginJar : pluginJars) {
            copyPluginToPluginsFolder(pluginJar);
        }

        try (URLClassLoader isolated = new URLClassLoader(new URL[]{serverJar.toUri().toURL()}, ServerInstance.class.getClassLoader().getParent())) {
            String manifest = getMainClassFromManifest(serverJar);
            Class<?> mainClass = isolated.loadClass(manifest);
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[]{"--nogui"});
        } catch (InvocationTargetException | IllegalAccessException | ClassNotFoundException | IOException |
                 NoSuchMethodException exception) {
            throw new RuntimeException("Failed to start server jar", exception);
        }

        return new ServerInstance(BukkitClassLoaderProvider.getServerClassLoader());
    }

    private void initializeServerProperties() {
        Path serverPropertiesFile = Paths.get("server.properties");
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : serverProperties.entrySet()) {
            lines.add(entry.getKey() + "=" + entry.getValue());
        }
        try {
            Files.write(serverPropertiesFile, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to write server.properties file", exception);
        }
    }

    private void copyPluginToPluginsFolder(Path pluginJar) {
        Path plugins = Paths.get("plugins");
        try {
            Files.createDirectories(plugins);
            Files.copy(pluginJar, plugins.resolve(pluginJar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to copy plugin jar to plugins folder: " + pluginJar, exception);
        }
    }


    static String getMainClassFromManifest(Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                Attributes mainAttributes = manifest.getMainAttributes();
                String mainClass = mainAttributes.getValue(Attributes.Name.MAIN_CLASS);
                if (mainClass != null) {
                    return mainClass;
                }
            }
            return "org.bukkit.craftbukkit.Main";
        }
    }
}
