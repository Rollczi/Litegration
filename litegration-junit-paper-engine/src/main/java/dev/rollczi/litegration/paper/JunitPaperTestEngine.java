package dev.rollczi.litegration.paper;

import dev.rollczi.litegration.Litegration;
import dev.rollczi.litegration.junit.LitegrationTest;
import dev.rollczi.litegration.paper.reflect.ReflectUtil;
import dev.rollczi.litegration.paper.server.ServerInstance;
import dev.rollczi.litegration.paper.classloader.RuntimeBridgeClassLoader;
import dev.rollczi.litegration.paper.downloader.PaperServerDownloader;
import dev.rollczi.litegration.paper.lock.LockPluginAccessor;
import dev.rollczi.litegration.paper.util.PortUtil;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import static org.junit.platform.engine.TestExecutionResult.*;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;

public class JunitPaperTestEngine implements TestEngine {

    public static final String PAPER_ENGINE_ID = "paper-engine";

    private final ClassLoader junitClassLoader = JunitPaperTestEngine.class.getClassLoader();
    private final RuntimeBridgeClassLoader bridgeClassLoader = RuntimeBridgeClassLoader.createForJunit(junitClassLoader)
        .withIgnoredPackages(List.of("dev.rollczi.litegration.Litegration"));

    private final JupiterTestEngine baseEngine = new JupiterTestEngine();

    @Override
    public String getId() {
        return PAPER_ENGINE_ID;
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
        JupiterConfiguration configuration = new CachingJupiterConfiguration(new DefaultJupiterConfiguration(
            request.getConfigurationParameters(),
            request.getOutputDirectoryCreator(),
            DiscoveryIssueReporter.forwarding(request.getDiscoveryListener(), uniqueId)
        ));
        JupiterEngineDescriptor engineDescriptor = new JupiterEngineDescriptor(uniqueId, configuration);

        for (ClassSelector selector : request.getSelectorsByType(ClassSelector.class)) {
            Class<?> testClass = selector.getJavaClass();
            List<Method> paperTests = Stream.of(testClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(LitegrationTest.class))
                .toList();

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


    @Override
    public void execute(ExecutionRequest request) {
        try {
            tryExecute(request);
        } catch (Throwable throwable) {
            request.getEngineExecutionListener().executionFinished(
                request.getRootTestDescriptor(),
                failed(throwable)
            );
            throw throwable;
        }
    }

    private void tryExecute(ExecutionRequest request) {
        Path testedPluginJar = resolvePlugin();
        LockPluginAccessor lock = LockPluginAccessor.createJar();
        int port = PortUtil.findFreePort();
        try (ServerInstance serverInstance = ServerInstance.withServer(fetchServerJar())
            .withPlugin(testedPluginJar)
            .withPlugin(lock.getPluginJar())
            .withPlugins(resolveExternalPlugins())
            .withServerProperty("server-port", String.valueOf(port))
            .withServerProperty("online-mode", "false")
            .withServerProperties(resolveServerProperties())
            .start()
        ) {
            lock.waitServerLoad(serverInstance);
            ClassLoader testedPluginClassLoader = serverInstance.findPluginClassLoader(PluginNameReader.read(testedPluginJar))
                .orElseThrow(() -> new RuntimeException("Failed to find plugin class loader for: " + testedPluginJar));
            bridgeClassLoader.withRuntimeLoader(testedPluginClassLoader);
            serverInstance.runInMainThread(() -> {
                Runnable deinitializer = Litegration.initialize("localhost", port);
                baseEngine.execute(remapRequest(request));
                deinitializer.run();
            }, lock.getPluginName());
        }
    }

    private ExecutionRequest remapRequest(ExecutionRequest request) {
        JupiterEngineDescriptor original = (JupiterEngineDescriptor) request.getRootTestDescriptor();
        JupiterConfiguration configuration = original.getConfiguration();
        JupiterEngineDescriptor reloaded = new JupiterEngineDescriptor(original.getUniqueId(), configuration);

        for (TestDescriptor child : original.getChildren()) {
            if (!(child instanceof ClassTestDescriptor originalClass)) {
                throw new RuntimeException("Unexpected child class descriptor: " + child.getClass());
            }

            Class<?> runtimedClass = ReflectUtil.getClass(bridgeClassLoader, originalClass.getTestClass().getName());
            TestDescriptor reloadedClass = new ClassTestDescriptor(
                originalClass.getUniqueId(),
                runtimedClass,
                configuration
            );

            for (TestDescriptor classChild : originalClass.getChildren()) {
                if (!(classChild instanceof TestMethodTestDescriptor originalMethod)) {
                    throw new RuntimeException("Unexpected method descriptor: " + classChild.getClass());
                }

                Method reloadedMethod = ReflectUtil.getMethod(
                    runtimedClass,
                    originalMethod.getTestMethod().getName(),
                    originalMethod.getTestMethod().getParameterTypes()
                );

                reloadedClass.addChild(new TestMethodTestDescriptor(
                    originalMethod.getUniqueId(),
                    runtimedClass,
                    reloadedMethod,
                    () -> List.of(),
                    configuration
                ));
            }

            reloaded.addChild(reloadedClass);
        }

        return ExecutionRequest.create(
            reloaded,
            request.getEngineExecutionListener(),
            request.getConfigurationParameters(),
            request.getOutputDirectoryCreator(),
            request.getStore(),
            request.getCancellationToken()
        );
    }

    private static Path fetchServerJar() {
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

    private static List<Path> resolveExternalPlugins() {
        String rawPlugins = System.getenv("LITEGRATION_EXTERNAL_PLUGINS");
        if (rawPlugins == null || rawPlugins.isBlank())
            return Collections.emptyList();
        return Stream.of(rawPlugins.split("\n"))
            .map(pluginJar -> pluginJar.trim())
            .filter(pluginJar -> !pluginJar.isBlank())
            .map(pluginJar -> Path.of(pluginJar))
            .toList();
    }

    private static Map<String, String> resolveServerProperties() {
        String rawProperties = System.getenv("LITEGRATION_SERVER_PROPERTIES");
        if (rawProperties == null || rawProperties.isBlank())
            return Collections.emptyMap();
        return Stream.of(rawProperties.split("\n"))
            .map(line -> line.trim())
            .filter(line -> !line.isBlank() && line.contains("="))
            .map(line -> line.split("=", 2))
            .collect(Collectors.toMap(parts -> parts[0].trim(), parts -> parts[1].trim()));
    }

}