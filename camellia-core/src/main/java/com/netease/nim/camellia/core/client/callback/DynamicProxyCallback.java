package com.netease.nim.camellia.core.client.callback;

import com.netease.nim.camellia.core.api.ReloadableProxyFactory;
import com.netease.nim.camellia.tools.utils.ExceptionUtils;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 *
 * Created by caojiajun on 2019/11/25.
 */
public class DynamicProxyCallback<T> implements MethodInterceptor {

    private final ReloadableProxyFactory<T> factory;

    public DynamicProxyCallback(ReloadableProxyFactory<T> factory) {
        this.factory = factory;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        try {
            return method.invoke(factory.getProxy(), args);
        } catch (Exception e) {
            throw ExceptionUtils.onError(e);
        }
    }
}
