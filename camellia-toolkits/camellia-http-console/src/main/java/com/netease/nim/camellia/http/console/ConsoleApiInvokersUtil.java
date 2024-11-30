package com.netease.nim.camellia.http.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by caojiajun on 2023/6/30
 */
public class ConsoleApiInvokersUtil {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleApiInvokersUtil.class);

    public static Map<String, ConsoleApiInvoker> initApiInvokers(IConsoleService service) {
        Class<? extends IConsoleService> clazz = service.getClass();
        Map<String, ConsoleApiInvoker> map = new HashMap<>();
        List<Method> methods = new ArrayList<>();
        findMethods(clazz, methods);

        for (final Method method : methods) {
            ConsoleApi annotation = method.getDeclaredAnnotation(ConsoleApi.class);
            if (annotation == null) {
                continue;
            }
            String uri = annotation.uri();
            Class<?> returnType = method.getReturnType();
            if (!ConsoleResult.class.isAssignableFrom(returnType)) {
                logger.warn("method with @ConsoleApi returnType is not ConsoleResult, skip");
                continue;
            }
            int parameterCount = method.getParameterCount();
            if (parameterCount == 0) {
                method.setAccessible(true);
                map.put(uri, new ConsoleApiInvoker(service, method, uri, false));
            } else if (parameterCount == 1) {
                Class<?> parameterType = method.getParameterTypes()[0];
                if (!Map.class.isAssignableFrom(parameterType)) {
                    logger.warn("method with @ConsoleApi parameter illegal, skip");
                    continue;
                }
                method.setAccessible(true);
                map.put(uri, new ConsoleApiInvoker(service, method, uri, true));
            } else {
                logger.warn("method with @ConsoleApi parameter illegal, skip");
            }
        }
        return map;
    }

    private static void findMethods(Class<?> clazz, List<Method> methods) {
        Method[] methods1 = clazz.getMethods();
        methods.addAll(Arrays.asList(methods1));
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            findMethods(superclass, methods);
        }
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> anInterface : interfaces) {
            findMethods(anInterface, methods);
        }
    }
}
