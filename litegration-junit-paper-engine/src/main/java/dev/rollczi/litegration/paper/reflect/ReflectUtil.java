package dev.rollczi.litegration.paper.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectUtil {

    public static <T> T readStaticField(Class<?> clazz, String fieldName) {
        try {
            Field serverReady = clazz.getDeclaredField(fieldName);
            serverReady.setAccessible(true);
            return (T) serverReady.get(null);
        }
        catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException("Failed to read static field: " + fieldName, exception);
        }
    }

    public static Class<?> getClass(ClassLoader classLoader, String className) {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load class: " + className, e);
        }
    }

    public static Method getMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            Method method = type.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to get method: " + name, e);
        }
    }

    public static Object invokeMethod(Object instance, String method, Object... args) {
        try {
            Method methodObj = getMethod(instance.getClass(), method, toClasses(args));
            return methodObj.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke method: " + method, e);
        }
    }

    public static Object invokeStaticMethod(Class<?> type, String name, Object... args) {
        try {
            Method method = getMethod(type, name, toClasses(args));
            return method.invoke(null, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke method: " + name, e);
        }
    }

    private static Class<?>[] toClasses(Object... args) {
        Class<?>[] classes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            classes[i] = args[i].getClass();
        }
        return classes;
    }

    public static Object invokeStaticMethod(Method method, Object... args) {
        try {
            return method.invoke(null, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke method: " + method.getName(), e);
        }
    }

    public static Object invokeMethod(Method method, Object instance, Object... args) {
        try {
            return method.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke method: " + method.getName(), e);
        }
    }
}
