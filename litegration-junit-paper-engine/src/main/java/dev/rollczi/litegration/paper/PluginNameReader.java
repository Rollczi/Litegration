package dev.rollczi.litegration.paper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.yaml.snakeyaml.Yaml;

final class PluginNameReader {

    private PluginNameReader() {
    }

    static String read(Path path) {
        try (JarFile jarFile = new JarFile(path.toFile())) {
            return readPluginName(jarFile)
                .orElseThrow(() -> new IllegalArgumentException("Could not find plugin.yml or paper-plugin.yml in " + path));
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read plugin name from " + path, e);
        }
    }

    private static Optional<String> readPluginName(JarFile jarFile) {
        JarEntry pluginYaml = Stream.of("plugin.yml", "paper-plugin.yml", "plugin.yaml", "paper-plugin.yaml")
            .map(name -> jarFile.getJarEntry(name))
            .filter(entry -> entry != null)
            .findFirst()
            .orElse(null);
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

            throw new IllegalArgumentException(pluginYaml.getRealName() + " does not contain a valid 'name' entry.");
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

}
