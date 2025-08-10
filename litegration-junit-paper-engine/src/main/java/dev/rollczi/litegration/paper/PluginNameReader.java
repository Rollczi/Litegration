package dev.rollczi.litegration.paper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.yaml.snakeyaml.Yaml;

final class PluginNameReader {

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
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(is);

            if (data != null && data.containsKey("name")) {
                Object nameObj = data.get("name");
                if (nameObj instanceof String name) {
                    if (!name.isEmpty()) {
                        return Optional.of(name);
                    }
                }
            }

            throw new IllegalArgumentException(fileName + " does not contain a valid 'name' entry.");
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

}
