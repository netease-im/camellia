package com.netease.nim.camellia.hotkey.tests.support;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ReflectionTestUtils {

    private ReflectionTestUtils() {
    }

    public static Object allocate(Class<?> clazz) {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);
            return unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, clazz);
        } catch (Exception e) {
            throw new IllegalStateException("allocate " + clazz.getName() + " failed", e);
        }
    }

    public static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new IllegalStateException("set field " + fieldName + " failed", e);
        }
    }

    public static Object getField(Object target, String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new IllegalStateException("get field " + fieldName + " failed", e);
        }
    }

    public static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = findMethod(target.getClass(), methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (Exception e) {
            throw new IllegalStateException("invoke method " + methodName + " failed", e);
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignore) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static Method findMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException ignore) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(methodName);
    }
}
