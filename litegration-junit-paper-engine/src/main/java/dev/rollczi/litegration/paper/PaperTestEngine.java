package dev.rollczi.litegration.paper;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.engine.config.CachingJupiterConfiguration;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;

public class PaperTestEngine implements TestEngine {

    public static final String PAPER_ENGINE_ID = "paper-engine";

    private final ClassLoader junitClassLoader = PaperTestEngine.class.getClassLoader();
    private final RuntimeBridgeClassLoader engineClassLoader = new RuntimeBridgeClassLoader(junitClassLoader.getParent())
        .withBridgedLoader(junitClassLoader);

    private final JupiterTestEngine baseEngine = new JupiterTestEngine();

    @Override
    public String getId() {
        return PAPER_ENGINE_ID;
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        Thread.currentThread().setContextClassLoader(engineClassLoader);
        JupiterConfiguration configuration = new CachingJupiterConfiguration(new DefaultJupiterConfiguration(
            discoveryRequest.getConfigurationParameters(), discoveryRequest.getOutputDirectoryProvider()));
        JupiterEngineDescriptor engineDescriptor = new JupiterEngineDescriptor(uniqueId, configuration);

        for (ClassSelector selector : discoveryRequest.getSelectorsByType(ClassSelector.class)) {
            Class<?> testClass = reloadClass(selector.getJavaClass());
            List<Method> paperTests = Stream.of(testClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(reloadClass(PaperTest.class)))
                .collect(Collectors.toList());

            if (paperTests.isEmpty()) {
                continue;
            }

            TestDescriptor classDescriptor = new ClassTestDescriptor(uniqueId.append("class", testClass.getName()), testClass, configuration);

            for (Method method : paperTests) {
                TestDescriptor methodDescriptor = new TestMethodTestDescriptor(
                    classDescriptor.getUniqueId().append("method", method.getName()),
                    testClass,
                    method,
                    () -> List.of(),
                    configuration
                );
                classDescriptor.addChild(methodDescriptor);
            }

            engineDescriptor.addChild(classDescriptor);
        }

        return engineDescriptor;
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> reloadClass(Class<T> appClass) {
        String className = appClass.getName();
        try {
            return (Class<T>) engineClassLoader.loadClass(className);
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void execute(ExecutionRequest request) {
        Path serverJar = resolveServer();
        Path pluginJar = resolvePlugin();
        try (ServerRunner serverRunner = new ServerRunner(serverJar)) {
            ClassLoader pluginClassLoader = serverRunner.findPluginClassLoader(pluginJar);
            engineClassLoader.withRuntimeLoader(pluginClassLoader);
            baseEngine.execute(request);
        }
        Thread.currentThread().setContextClassLoader(junitClassLoader);
    }

    private static Path resolveServer() {
        String serverVersion = System.getenv("LITEGRATION_SERVER_VERSION");
        if (serverVersion == null || serverVersion.isBlank())
            throw new RuntimeException("No server version found! Please configure 'serverVersion = ' in your build.gradle file.");
        return PaperServerDownloader.download(serverVersion);
    }

    private static Path resolvePlugin() {
        String pluginsJar = System.getenv("LITEGRATION_PLUGIN");
        if (pluginsJar == null || pluginsJar.isBlank())
            throw new RuntimeException("No plugins found! Please configure 'plugins = ' in your build.gradle file.");
        return Path.of(pluginsJar);
    }

}