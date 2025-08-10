package dev.rollczi.litegration.paper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class PluginNameReader {

    private PluginNameReader() {
    }

    static String read(Path path) {
        try (JarFile jarFile = new JarFile(path.toFile())) {
            return readPluginName(jarFile, "plugin.yml")
                .or(() -> readPluginName(jarFile, "paper-plugin.yaml"))
                .orElseThrow(() -> new IllegalArgumentException("Could not find plugin.yml or paper-plugin.yml in " + path));
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read plugin name from " + path, e);
        }
    }

    private static Optional<String> readPluginName(JarFile jarFile, String fileName) {
        JarEntry pluginYaml = jarFile.getJarEntry(fileName);
        if (pluginYaml == null) {
            return Optional.empty();
        }

        try (InputStream is = jarFile.getInputStream(pluginYaml)) {
            Properties properties = new Properties();
            properties.load(is);
            String name = properties.getProperty("name");
            if (name != null && !name.isEmpty()) {
                return Optional.of(name);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        return Optional.empty();
    }

}
