package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.redis.proxy.command.Command;

import java.lang.reflect.Method;

public class CommandInvokerUtil {

    public static Object invoke(Method method, Command command, Object processor) throws Exception {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];
        command.fillParameters(parameterTypes, parameters);
        return method.invoke(processor, parameters);
    }
}

