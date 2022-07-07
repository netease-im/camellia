package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.client.annotation.ReadOp;
import com.netease.nim.camellia.core.client.annotation.WriteOp;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by caojiajun on 2022/3/28
 */
public class ReadWriteOperationCache {

    public static final byte WRITE = 1;
    public static final byte READ = 2;
    public static final byte UNKNOWN = 3;

    private final Map<Method, Byte> annotationCache = new HashMap<>();
    private final Map<Method, String> fullNameCache = new HashMap<>();
    private final Map<Method, String> genericStringCache = new HashMap<>();

    public void preheat(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            getOperationType(method);
            getMethodName(method);
        }
    }

    public byte getOperationType(Method method) {
        if (method == null) return UNKNOWN;
        Byte cache = annotationCache.get(method);
        if (cache != null) return cache;
        WriteOp writeOp = method.getAnnotation(WriteOp.class);
        if (writeOp != null) {
            annotationCache.put(method, WRITE);
            return WRITE;
        }
        ReadOp readOp = method.getAnnotation(ReadOp.class);
        if (readOp != null) {
            annotationCache.put(method, READ);
            return READ;
        }
        annotationCache.put(method, UNKNOWN);
        return UNKNOWN;
    }

    public String getMethodName(Method method) {
        String string = fullNameCache.get(method);
        if (string != null) {
            return string;
        }
        StringBuilder fullName = new StringBuilder();
        String name = method.getName();
        fullName.append(name);
        Class<?>[] parameterTypes = method.getParameterTypes();
        fullName.append("(");
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            if (i != parameterTypes.length - 1) {
                fullName.append(type.getSimpleName());
                fullName.append(",");
            } else {
                fullName.append(type.getSimpleName());
            }
        }
        fullName.append(")");
        string = fullName.toString();
        fullNameCache.put(method, string);
        return string;
    }

    public String getGenericString(Method method) {
        String string = genericStringCache.get(method);
        if (string != null) {
            return string;
        }
        String genericString = method.toGenericString();
        genericStringCache.put(method, genericString);
        return genericString;
    }
}
