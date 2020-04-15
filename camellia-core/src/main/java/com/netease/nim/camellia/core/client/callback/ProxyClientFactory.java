package com.netease.nim.camellia.core.client.callback;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;


/**
 *
 * Created by caojiajun on 2019/5/16.
 */
public class ProxyClientFactory {

    public static <T> T createProxy(Class<T> clazz, Class[] argumentTypes, final Object[] arguments, MethodInterceptor callback) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(callback);
        return (T) enhancer.create(argumentTypes, arguments);
    }
}
