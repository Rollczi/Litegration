package dev.rollczi.litegration.paper.server;

import dev.rollczi.litegration.paper.reflect.ReflectUtil;
import java.nio.file.Path;
import java.util.Optional;
import org.bukkit.Bukkit;

public class ServerInstance implements AutoCloseable {

    private final ClassLoader serverClassLoader;

    public ServerInstance(ClassLoader serverClassLoader) {
        this.serverClassLoader = serverClassLoader;
    }

    public Optional<ClassLoader> findPluginClassLoader(String pluginName) {
        Class<?> bukkitClass = ReflectUtil.getClass(serverClassLoader, Bukkit.class.getName());
        Object pluginManager = ReflectUtil.invokeStaticMethod(bukkitClass, "getPluginManager");
        if (pluginManager == null)
            return Optional.empty();
        Object plugin = ReflectUtil.invokeMethod(pluginManager, "getPlugin", pluginName);
        if (plugin == null)
            return Optional.empty();
        return Optional.of(plugin.getClass().getClassLoader());
    }

    public static ServerConfigurer withServer(Path serverJar) {
        return new ServerConfigurer(serverJar);
    }

    @Override
    public void close() {
        Class<?> bukkitClass = ReflectUtil.getClass(serverClassLoader, "org.bukkit.Bukkit");
        Object server = ReflectUtil.invokeStaticMethod(bukkitClass, "getServer");
        if (server == null) {
            throw  new RuntimeException("Server class not found!");
        }
        ReflectUtil.invokeMethod(server, "shutdown");
    }

}
