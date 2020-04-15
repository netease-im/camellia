package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.redis.proxy.enums.CommandFinder;

import java.lang.reflect.Method;
import java.util.Map;

/**
 *
 * Created by popo on 2020/2/27.
 */
public class CommandMethodUtil {

    public static void initCommandFinderMethods(Class clazz, Map<String, Method> map) {
        for (final Method method : clazz.getMethods()) {
            CommandFinder annotation = method.getAnnotation(CommandFinder.class);
            if (annotation != null) {
                String commandName = annotation.value().name().toLowerCase();
                map.put(commandName, method);
            }
        }
    }
}
