package dev.rollczi.litegration.paper.lock;

import dev.rollczi.litegration.paper.reflect.ReflectUtil;
import dev.rollczi.litegration.paper.server.ServerInstance;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class LockPluginAccessor {

    private final Path internalPluginJar;

    public LockPluginAccessor(Path internalPluginJar) {
        this.internalPluginJar = internalPluginJar;
    }

    public void waitServerLoad(ServerInstance server) {
        ClassLoader lockPluginLoader = waitForPluginClassLoader(server, LockLitegrationPlugin.class.getSimpleName());
        Class<?> loaded = ReflectUtil.getClass(lockPluginLoader, LockLitegrationPlugin.class.getName());
        CompletableFuture<Boolean> serverReady = ReflectUtil.readStaticField(loaded, "SERVER_READY");
        CompletableFuture<Boolean> clientReady = ReflectUtil.readStaticField(loaded, "CLIENT_READY");

        serverReady.join();
        clientReady.complete(true);
    }

    public static ClassLoader waitForPluginClassLoader(ServerInstance server, String pluginName) {
        while (true) {
            try {
                Optional<ClassLoader> loader = server.findPluginClassLoader(pluginName);
                if (loader.isPresent()) {
                    return loader.get();
                }
                Thread.sleep(50);
            } catch (RuntimeException exception) {
                if (exception.getCause() instanceof InvocationTargetException targetException && targetException.getTargetException() instanceof NullPointerException)
                    continue;
                throw exception;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for plugin class loader: " + pluginName, e);
            }
        }
    }

    public Path getPluginJar() {
        return internalPluginJar;
    }

    public static LockPluginAccessor createJar() {
        try {
            Path pluginsDir = Paths.get("plugins");
            Files.createDirectories(pluginsDir);
            Path pluginJar = pluginsDir.resolve("litegration-internal-plugin.jar");

            try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(pluginJar))) {
                addPluginYaml(output);
                addClassToJar(output, LockLitegrationPlugin.class);
            }
            return new LockPluginAccessor(pluginJar);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to create internal plugin JAR", exception);
        }
    }

    private static void addPluginYaml(JarOutputStream output) throws IOException {
        output.putNextEntry(new ZipEntry("plugin.yml"));
        try (InputStream is = LockPluginAccessor.class.getResourceAsStream("/litegration-internal-plugin.yml")) {
            if (is == null) {
                throw new IOException("Cannot find resource: /litegration-internal-plugin.yml");
            }
            is.transferTo(output);
        }
        output.closeEntry();
    }

    private static void addClassToJar(JarOutputStream jos, Class<?> clazz) throws IOException {
        String classPath = clazz.getName().replace('.', '/') + ".class";
        jos.putNextEntry(new ZipEntry(classPath));
        try (InputStream is = LockPluginAccessor.class.getResourceAsStream("/" + classPath)) {
            if (is == null) {
                throw new IOException("Cannot find class resource: " + classPath);
            }
            is.transferTo(jos);
        }
        jos.closeEntry();
    }

}
