package dev.rollczi.litegration.paper.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A ClassLoader that acts as a bridge between two class loaders:
 * a "bridged" loader, which provides raw class data without runtime access,
 * and a "runtime" loader which provides full runtime class loading capabilities.
 * <p>
 * This loader attempts to load classes and resources first from the runtime loader,
 * and if not found, it manually defines classes from the bridged loader,
 * effectively granting the bridged classes access to the runtime classes.
 * </p>
 */
public class RuntimeBridgeClassLoader extends ClassLoader {

    private ClassLoader bridgedClassSourceLoader;
    private ClassLoader runtimeLoader;
    private List<String> ignoredPackages = new ArrayList<>();

    public RuntimeBridgeClassLoader(ClassLoader parent) {
        super(parent);
    }

    public RuntimeBridgeClassLoader withBridgedSourceLoader(ClassLoader classSourceLoader) {
        this.bridgedClassSourceLoader = classSourceLoader;
        return this;
    }

    public RuntimeBridgeClassLoader withRuntimeLoader(ClassLoader runtimeLoader) {
        this.runtimeLoader = runtimeLoader;
        return this;
    }

    public RuntimeBridgeClassLoader withIgnoredPackages(List<String> ignoredPackages) {
        this.ignoredPackages = ignoredPackages;
        return this;
    }

    public ClassLoader getBridgedClassSourceLoader() {
        return bridgedClassSourceLoader;
    }

    public ClassLoader getRuntimeLoader() {
        return runtimeLoader;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        if (runtimeLoader != null) {
            try {
                return runtimeLoader.loadClass(name);
            } catch (ClassNotFoundException ignore) {
            }
        }

        if (bridgedClassSourceLoader != null && isIgnoredPackage(name)) {
            return bridgedClassSourceLoader.loadClass(name);
        }

        return tryBridgeClass(name);
    }

    private boolean isIgnoredPackage(String className) {
        for (String ignoredPackage : ignoredPackages) {
            if (className.startsWith(ignoredPackage)) {
                return true;
            }
        }
        return false;
    }

    private Class<?> tryBridgeClass(String name) throws ClassNotFoundException {
        try {
            String path = name.replace('.', '/') + ".class";
            try (InputStream is = bridgedClassSourceLoader.getResourceAsStream(path)) {
                if (is != null) {
                    byte[] classData = is.readAllBytes();
                    return defineClass(name, classData, 0, classData.length);
                }
            }
        } catch (IOException ignore) {
        }

        throw new ClassNotFoundException(name);
    }

    @Override
    protected URL findResource(String name) {
        if (runtimeLoader != null) {
            URL supplied = runtimeLoader.getResource(name);
            if (supplied != null) {
                return supplied;
            }
        }
        return bridgedClassSourceLoader.getResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        List<ClassLoader> classLoaders = new ArrayList<>();
        if (runtimeLoader != null) {
            classLoaders.add(runtimeLoader);
        }
        classLoaders.add(bridgedClassSourceLoader);
        return new MergedEnumeration(classLoaders, name);
    }

    private static class MergedEnumeration implements Enumeration<URL> {

        private final Iterator<ClassLoader> classLoaderIterator;
        private final String resourceName;

        private Enumeration<URL> currentResources;

        public MergedEnumeration(List<ClassLoader> classLoaders, String resourceName) throws IOException {
            this.classLoaderIterator = classLoaders.iterator();
            this.resourceName = resourceName;
            this.currentResources = Collections.emptyEnumeration();
            findNextResources();
        }

        private void findNextResources() throws IOException {
            while (!currentResources.hasMoreElements() && classLoaderIterator.hasNext()) {
                ClassLoader nextClassLoader = classLoaderIterator.next();
                currentResources = nextClassLoader.getResources(resourceName);
            }
        }

        @Override
        public boolean hasMoreElements() {
            if (currentResources.hasMoreElements()) {
                return true;
            }

            try {
                findNextResources();
            } catch (IOException e) {
                return false;
            }
            return currentResources.hasMoreElements();
        }
        @Override
        public URL nextElement() {
            if (!hasMoreElements()) {
                throw new NoSuchElementException();
            }
            return currentResources.nextElement();
        }

    }

    public static RuntimeBridgeClassLoader createForJunit(ClassLoader junitClassLoader) {
        ClassLoader appClassLoader = junitClassLoader.getParent();
        return new RuntimeBridgeClassLoader(appClassLoader)
            .withBridgedSourceLoader(junitClassLoader);
    }

}
