package dev.rollczi.litegration.paper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

class ServerRunner implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(ServerRunner.class.getName());

    private final ExecutorService serverThread = Executors.newSingleThreadExecutor(action -> {
        Thread thread = new Thread(action);
        thread.setName("Litegration-Server-Thread");
        return thread;
    });

    public ServerRunner(Path serverJar) {
        if (!serverJar.toFile().exists()) {
            throw new IllegalArgumentException("Server jar file does not exist: " + serverJar.toAbsolutePath());
        }
        this.start(serverJar);
    }

    public ClassLoader findPluginClassLoader(Path pluginJar) {
        return PluginLoaderFinder.findPluginClassLoader(pluginJar);
    }

    private void start(Path serverJar) {
        try {
            CountDownLatch serverReady = startServerJar(serverJar);

            LOGGER.info("> Waiting for server to start...");
            if (!serverReady.await(5, TimeUnit.MINUTES)) {
                throw new RuntimeException("Server did not start in time!");
            }

            LOGGER.info("> Server started successfully.");
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }

    private CountDownLatch startServerJar(Path serverJar) throws IOException {
        String manifest = getMainClassFromManifest(serverJar);
        LOGGER.info("> Starting server from: " + serverJar.toAbsolutePath() + " with main class: " + manifest);

        CountDownLatch serverReady = new CountDownLatch(1);
        serverThread.submit(() -> {
            try (URLClassLoader isolated = new URLClassLoader(new URL[]{serverJar.toUri().toURL()}, ServerRunner.class.getClassLoader().getParent())) {
                Thread.currentThread().setContextClassLoader(isolated);
                System.setOut(new PrintStream(new LogRedirector(System.out, serverReady), true));
                System.setErr(new PrintStream(new LogRedirector(System.err, null), true));

                Class<?> mainClass = isolated.loadClass(manifest);
                Method mainMethod = mainClass.getMethod("main", String[].class);
                mainMethod.invoke(null, (Object) new String[]{"--nogui"});

            } catch (RuntimeException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException |
                     IllegalAccessException | IOException exception) {
                LOGGER.log(Level.SEVERE, "> Error during server startup", exception);
            }
        });
        return serverReady;
    }

    @Override
    public void close() {
        try {
            ClassLoader serverClassLoader = PluginLoaderFinder.findServerClassLoader();
            Class<?> bukkitClass = serverClassLoader.loadClass("org.bukkit.Bukkit");
            Object server = bukkitClass.getMethod("getServer").invoke(null);

            if (server != null) {
                LOGGER.info("> Shutting down server - invoke -> Bukkit.getServer().shutdown()");
                Method shutdownMethod = server.getClass().getMethod("shutdown");
                shutdownMethod.invoke(server);
            }

            serverThread.shutdown();
            if (serverThread.awaitTermination(1, TimeUnit.MINUTES)) {
                LOGGER.info("> Server shutdown successfully.");
                return;
            }

            LOGGER.warning("> Server did not terminate in time. Shutting down now.");
            serverThread.shutdownNow();
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "> Error during server shutdown", exception);
        }
    }

    private String getMainClassFromManifest(Path jarPath) throws IOException {
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

    private static class LogRedirector extends OutputStream {
        private final PrintStream original;
        private final CountDownLatch latch;
        private final StringBuilder lineBuilder = new StringBuilder();

        public LogRedirector(PrintStream original, CountDownLatch latch) {
            this.original = original;
            this.latch = latch;
        }

        @Override
        public void write(int b) {
            original.write(b);
            if (b == '\n') {
                String line = lineBuilder.toString();
                if (latch != null && line.contains("Done (")) {
                    latch.countDown();
                }
                lineBuilder.setLength(0);
            } else {
                lineBuilder.append((char) b);
            }
        }
    }
}