package dev.rollczi.litegration.paper.server;

import java.util.List;
import java.util.Optional;

class BukkitClassLoaderProvider {

    private static final List<String> POTENTIAL_MINECRAFT_THREAD_NAMES = List.of("Paper", "Craft");

    static ClassLoader getServerClassLoader() {
        while (true) {
            Optional<ClassLoader> classLoader = Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> POTENTIAL_MINECRAFT_THREAD_NAMES.stream().anyMatch(potential -> thread.getName().contains(potential)))
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
