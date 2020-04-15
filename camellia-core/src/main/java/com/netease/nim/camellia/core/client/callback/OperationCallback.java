package com.netease.nim.camellia.core.client.callback;

import com.netease.nim.camellia.core.client.annotation.ReadOp;
import com.netease.nim.camellia.core.client.annotation.WriteOp;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceReadOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;
import com.netease.nim.camellia.core.util.CheckUtil;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 * Created by caojiajun on 2019/5/16.
 */
public class OperationCallback<T> implements MethodInterceptor {

    private ResourceOperation resourceOperation;
    private Map<Resource, T> clientMap;
    private String className;
    private ProxyEnv env = ProxyEnv.defaultProxyEnv();

    public OperationCallback(ResourceOperation resourceOperation, Map<Resource, T> clientMap, Class<T> clazz, ProxyEnv env) {
        if (resourceOperation == null) {
            throw new IllegalArgumentException("resourceOperation is null");
        }
        if (clientMap == null) {
            throw new IllegalArgumentException("clientMap is null");
        }
        if (!CheckUtil.checkResourceOperation(resourceOperation)) {
            throw new IllegalArgumentException("resourceOperation check fail");
        }
        check(resourceOperation, clientMap);
        for (Method method : clazz.getMethods()) {
            operationType(method);
            getMethodName(method);
        }
        this.resourceOperation = resourceOperation;
        this.clientMap = clientMap;
        this.className = clazz.getName();
        if (env != null) {
            this.env = env;
        }
    }

    private static final byte WRITE = 1;
    private static final byte READ = 2;
    private static final byte UNKNOWN = 3;
    private Map<Method, Byte> annotationCache = new HashMap<>();

    private byte operationType(Method method) {
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

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        Byte operationType = operationType(method);
        if (Objects.equals(operationType, WRITE)) {
            return write(objects, method);
        }
        if (Objects.equals(operationType, READ)) {
            return read(objects, method);
        }
        return methodProxy.invokeSuper(o, objects);
    }

    private Object write(final Object[] objects, final Method method) throws Throwable {
        ResourceOperation.Type type = resourceOperation.getType();
        switch (type) {
            case SIMPLE:
                Resource resource1 = resourceOperation.getResource();
                T client = clientMap.get(resource1);
                incrWrite(resource1, method);
                return method.invoke(client, objects);
            case RW_SEPARATE:
                ResourceWriteOperation writeOperation = resourceOperation.getWriteOperation();
                switch (writeOperation.getType()) {
                    case SIMPLE:
                        Resource resource2 = writeOperation.getWriteResource();
                        T client1 = clientMap.get(resource2);
                        incrWrite(resource2, method);
                        return method.invoke(client1, objects);
                    case MULTI:
                        if (env.isMultiWriteConcurrentEnable()) {
                            List<Future> futureList = new ArrayList<>();
                            for (final Resource resource : writeOperation.getWriteResources()) {
                                final T client2 = clientMap.get(resource);
                                Future<Object> future = env.getMultiWriteConcurrentExec().submit(new Callable<Object>() {
                                    @Override
                                    public Object call() throws Exception {
                                        incrWrite(resource, method);
                                        return method.invoke(client2, objects);
                                    }
                                });
                                futureList.add(future);
                            }
                            Object ret = null;
                            boolean isRetSet = false;
                            for (Future future : futureList) {
                                Object ret1 = future.get();
                                if (!isRetSet) {
                                    ret = ret1;
                                    isRetSet = true;
                                }
                            }
                            return ret;
                        } else {
                            Object ret = null;
                            boolean isRetSet = false;
                            for (Resource resource : writeOperation.getWriteResources()) {
                                T client2 = clientMap.get(resource);
                                incrWrite(resource, method);
                                Object ret1 = method.invoke(client2, objects);
                                if (!isRetSet) {
                                    ret = ret1;
                                    isRetSet = true;
                                }
                            }
                            return ret;
                        }
                    default:
                        throw new RuntimeException("unknown operation write type");
                }
            default:
                throw new IllegalArgumentException("unknown operation type");
        }
    }

    private Object read(Object[] objects, Method method) throws Throwable {
        ResourceOperation.Type type = resourceOperation.getType();
        switch (type) {
            case SIMPLE:
                Resource resource1 = resourceOperation.getResource();
                T client = clientMap.get(resource1);
                incrRead(resource1, method);
                return method.invoke(client, objects);
            case RW_SEPARATE:
                ResourceReadOperation readOperation = resourceOperation.getReadOperation();
                switch (readOperation.getType()) {
                    case SIMPLE:
                        Resource resource2 = readOperation.getReadResource();
                        T client1 = clientMap.get(resource2);
                        incrRead(resource2, method);
                        return method.invoke(client1, objects);
                    case ORDER:
                        Throwable ex = null;
                        for (Resource resource : readOperation.getReadResources()) {
                            T client2 = clientMap.get(resource);
                            try {
                                incrRead(resource, method);
                                return method.invoke(client2, objects);
                            } catch (Throwable throwable) {
                                ex = throwable;
                            }
                        }
                        if (ex != null) {
                            throw ex;
                        }
                        throw new RuntimeException("no reachable read client");
                    case RANDOM:
                        List<Resource> list = readOperation.getReadResources();
                        int index = ThreadLocalRandom.current().nextInt(list.size());
                        Resource resource = list.get(index);
                        T client3 = clientMap.get(resource);
                        incrRead(resource, method);
                        return method.invoke(client3, objects);
                    default:
                        throw new RuntimeException("unknown operation read type");
                }
            default:
                throw new IllegalArgumentException("unknown operation type");
        }
    }

    private Map<Method, String> fullNameCache = new HashMap<>();

    private String getMethodName(Method method) {
        String string = fullNameCache.get(method);
        if (string != null) {
            return string;
        }
        StringBuilder fullName = new StringBuilder();
        String name = method.getName();
        fullName.append(name);
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes == null) {
            fullName.append("()");
        } else {
            fullName.append("(");
            for (int i = 0; i < parameterTypes.length; i++) {
                Class type = parameterTypes[i];
                if (i != parameterTypes.length - 1) {
                    fullName.append(type.getSimpleName());
                    fullName.append(",");
                } else {
                    fullName.append(type.getSimpleName());
                }
            }
            fullName.append(")");
        }
        string = fullName.toString();
        fullNameCache.put(method, string);
        return string;
    }

    private void incrWrite(Resource resource, Method method) {
        if (env != null && env.getMonitor() != null) {
            env.getMonitor().incrWrite(resource.getUrl(), className, getMethodName(method));
        }
    }

    private void incrRead(Resource resource, Method method) {
        if (env != null && env.getMonitor() != null) {
            env.getMonitor().incrRead(resource.getUrl(), className, getMethodName(method));
        }
    }

    private void check(ResourceOperation resourceOperation, Map<Resource, T> clientMap) {
        ResourceOperation.Type type = resourceOperation.getType();
        switch (type) {
            case SIMPLE:
                if (!clientMap.containsKey(resourceOperation.getResource())) {
                    throw new IllegalArgumentException("resourceOperation/clientMap not match");
                }
                break;
            case RW_SEPARATE:
                ResourceWriteOperation writeOperation = resourceOperation.getWriteOperation();
                switch (writeOperation.getType()) {
                    case SIMPLE:
                        if (!clientMap.containsKey(writeOperation.getWriteResource())) {
                            throw new IllegalArgumentException("resourceOperation/clientMap not match");
                        }
                        break;
                    case MULTI:
                        for (Resource resource : writeOperation.getWriteResources()) {
                            if (!clientMap.containsKey(resource)) {
                                throw new IllegalArgumentException("resourceOperation/clientMap not match");
                            }
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("resourceOperation/clientMap not match");
                }
                ResourceReadOperation readOperation = resourceOperation.getReadOperation();
                switch (readOperation.getType()) {
                    case SIMPLE:
                        if (!clientMap.containsKey(readOperation.getReadResource())) {
                            throw new IllegalArgumentException("resourceOperation/clientMap not match");
                        }
                        break;
                    case RANDOM:
                    case ORDER:
                        for (Resource resource : readOperation.getReadResources()) {
                            if (!clientMap.containsKey(resource)) {
                                throw new IllegalArgumentException("resourceOperation/clientMap not match");
                            }
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("resourceOperation/clientMap not match");
                }
        }
    }
}
