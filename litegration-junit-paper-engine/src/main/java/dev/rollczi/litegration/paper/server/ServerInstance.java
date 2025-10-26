package dev.rollczi.litegration.paper.server;

import dev.rollczi.litegration.paper.reflect.ReflectUtil;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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

    public void runInMainThread(Runnable runnable, String pluginName) {
        Class<?> bukkitClass = ReflectUtil.getClass(serverClassLoader, "org.bukkit.Bukkit");
        Class<?> pluginClass = ReflectUtil.getClass(serverClassLoader, "org.bukkit.plugin.Plugin");
        CompletableFuture<Object> future = new CompletableFuture<>();
        Runnable finalRunnable = () -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        };
        Object pluginManager = ReflectUtil.invokeStaticMethod(bukkitClass, "getPluginManager");
        Object plugin = ReflectUtil.invokeMethod(pluginManager, "getPlugin", pluginName);
        if (plugin == null) {
            throw new RuntimeException("Plugin " + pluginName + " not found!");
        }
        Object scheduler = ReflectUtil.invokeStaticMethod(bukkitClass, "getScheduler");
        Method runTask = ReflectUtil.getMethod(scheduler.getClass(), "runTask", pluginClass, Runnable.class);
        try {
            runTask.invoke(scheduler, plugin, finalRunnable);
        } catch (Exception e) {
            throw new RuntimeException("Failed to schedule task on main thread", e);
        }
        future.join();
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

    public static ServerConfigurer withServer(Path serverJar) {
        return new ServerConfigurer(serverJar);
    }

}
