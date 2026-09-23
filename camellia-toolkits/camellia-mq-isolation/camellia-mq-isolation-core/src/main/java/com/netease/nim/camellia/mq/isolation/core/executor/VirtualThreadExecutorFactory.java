package com.netease.nim.camellia.mq.isolation.core.executor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Keeps camellia-mq-isolation-core loadable on Java 8 while allowing Java 21
 * callers to opt in to virtual threads.
 */
final class VirtualThreadExecutorFactory {

    private VirtualThreadExecutorFactory() {
    }

    static ExecutorService create() {
        try {
            Method method = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            Object executor = method.invoke(null);
            if (!(executor instanceof ExecutorService)) {
                throw new IllegalStateException("virtual thread factory returned an invalid executor");
            }
            return (ExecutorService) executor;
        } catch (NoSuchMethodException e) {
            throw unsupported(e);
        } catch (IllegalAccessException e) {
            throw unsupported(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw unsupported(cause);
        } catch (RuntimeException e) {
            throw unsupported(e);
        } catch (Error e) {
            throw unsupported(e);
        }
    }

    static void validate() {
        ExecutorService executor = create();
        executor.shutdown();
    }

    private static IllegalStateException unsupported(Throwable cause) {
        return new IllegalStateException("virtual threads are unavailable, java.version="
                + System.getProperty("java.version"), cause);
    }
}
