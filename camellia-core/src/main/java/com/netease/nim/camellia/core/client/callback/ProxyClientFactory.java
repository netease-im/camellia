package com.netease.nim.camellia.core.client.callback;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 *
 * Created by caojiajun on 2019/5/16.
 */
public class ProxyClientFactory {

    public static <T> T createProxy(Class<T> clazz, Class<?>[] argumentTypes, final Object[] arguments, MethodInterceptor callback) {
        if (clazz.isInterface()) {
            return createInterfaceProxy(clazz, callback);
        }
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(callback);
        return (T) enhancer.create(argumentTypes, arguments);
    }

    public static <T> T createProxy(Class<T> clazz, MethodInterceptor callback) {
        if (clazz.isInterface()) {
            return createInterfaceProxy(clazz, callback);
        }
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(callback);
        return (T) enhancer.create();
    }

    private static <T> T createInterfaceProxy(Class<T> clazz, MethodInterceptor callback) {
        Object proxy = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] {clazz},
                new InterfaceInvocationHandler(clazz, callback));
        return (T) proxy;
    }

    private static class InterfaceInvocationHandler implements InvocationHandler {

        private final Class<?> clazz;
        private final MethodInterceptor callback;

        private InterfaceInvocationHandler(Class<?> clazz, MethodInterceptor callback) {
            this.clazz = clazz;
            this.callback = callback;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return invokeObjectMethod(proxy, method, args);
            }
            if (args == null) {
                args = new Object[0];
            }
            return callback.intercept(proxy, method, args, null);
        }

        private Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("toString".equals(name)) {
                return clazz.getName() + " proxy@" + Integer.toHexString(System.identityHashCode(proxy));
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            throw new UnsupportedOperationException(method.toString());
        }
    }
}
