package dev.rollczi.litegration.paper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

class PluginLoaderFinder {

    static ClassLoader findPluginClassLoader(Path path) {
        try {
            String pluginName = PluginNameReader.read(path);
            ClassLoader junitClassLoader = PluginLoaderFinder.class.getClassLoader();
            RuntimeBridgeClassLoader loader = new RuntimeBridgeClassLoader(junitClassLoader.getParent())
                .withBridgedLoader(junitClassLoader)
                .withRuntimeLoader(findServerClassLoader());

            Class<?> internalFinder = loader.loadClass(PluginInternalLoaderFinder.class.getName());
            Method findPluginClassLoader = internalFinder.getDeclaredMethod("findPluginClassLoader", String.class);
            findPluginClassLoader.setAccessible(true);
            return (ClassLoader) findPluginClassLoader.invoke(null, pluginName);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException exception) {
            throw new RuntimeException(exception);
        }
    }

    static ClassLoader findServerClassLoader() {
        List<String> potentialMinecraftThreadNames = List.of("Paper", "Craft");
        while (true) {
            Optional<ClassLoader> classLoader = Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> potentialMinecraftThreadNames.stream().anyMatch(potential -> thread.getName().contains(potential)))
                .map(thread -> thread.getContextClassLoader())
                .filter(loader -> isBukkitLoader(loader))
                .findFirst();
            if (classLoader.isPresent()) {
                return classLoader.get();
            }
            Thread.onSpinWait();
        }
    }

    private static boolean isBukkitLoader(ClassLoader loader) {
        try {
            loader.loadClass("org.bukkit.Bukkit");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

}
