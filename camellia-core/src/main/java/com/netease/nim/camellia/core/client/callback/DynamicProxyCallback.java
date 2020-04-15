package com.netease.nim.camellia.core.client.callback;

import com.netease.nim.camellia.core.api.ReloadableProxyFactory;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 *
 * Created by caojiajun on 2019/11/25.
 */
public class DynamicProxyCallback implements MethodInterceptor {

    private ReloadableProxyFactory factory;

    public DynamicProxyCallback(ReloadableProxyFactory factory) {
        this.factory = factory;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        return method.invoke(factory.getProxy(), args);
    }
}
