package com.netease.nim.camellia.core.client.callback;

import com.netease.nim.camellia.core.client.env.MultiWriteType;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.env.ThreadContextSwitchStrategy;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceReadOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;
import com.netease.nim.camellia.core.util.CheckUtil;
import com.netease.nim.camellia.core.util.ExceptionUtils;
import com.netease.nim.camellia.core.util.ReadWriteOperationCache;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 * Created by caojiajun on 2019/5/16.
 */
public class OperationCallback<T> implements MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(OperationCallback.class);

    private final ResourceOperation resourceOperation;
    private final Map<Resource, T> clientMap;
    private final String className;
    private ProxyEnv env = ProxyEnv.defaultProxyEnv();
    private final ReadWriteOperationCache readWriteOperationCache = new ReadWriteOperationCache();

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
        readWriteOperationCache.preheat(clazz);
        this.resourceOperation = resourceOperation;
        this.clientMap = clientMap;
        this.className = clazz.getName();
        if (env != null) {
            this.env = env;
        }
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        try {
            byte operationType = readWriteOperationCache.getOperationType(method);
            if (operationType == ReadWriteOperationCache.WRITE) {
                return write(objects, method);
            }
            if (operationType == ReadWriteOperationCache.READ) {
                return read(objects, method);
            }
            return methodProxy.invokeSuper(o, objects);
        } catch (Throwable e) {
            throw ExceptionUtils.onError(e);
        }
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
                        List<Resource> writeResources = writeOperation.getWriteResources();
                        if (writeResources.size() == 1) {
                            Resource resource3 = writeResources.get(0);
                            T client2 = clientMap.get(resource3);
                            incrWrite(resource3, method);
                            return method.invoke(client2, objects);
                        }
                        MultiWriteType multiWriteType = env.getMultiWriteType();
                        if (multiWriteType == MultiWriteType.MULTI_THREAD_CONCURRENT) {
                            ThreadContextSwitchStrategy strategy = env.getThreadContextSwitchStrategy();
                            List<Future<Object>> futureList = new ArrayList<>();
                            for (Resource resource : writeOperation.getWriteResources()) {
                                final T client2 = clientMap.get(resource);
                                Future<Object> future = env.getMultiWriteConcurrentExec().submit(strategy.wrapperCallable(() -> {
                                    try {
                                        incrWrite(resource, method);
                                        return method.invoke(client2, objects);
                                    } catch (Exception e) {
                                        logger.error("multi thread concurrent invoke error, class = {}, method = {}, resource = {}",
                                                className, method.getName(), resource.getUrl(), e);
                                        throw e;
                                    }
                                }));
                                futureList.add(future);
                            }
                            Object ret = null;
                            for (int i=0; i<futureList.size(); i++) {
                                Future<Object> future = futureList.get(i);
                                Object obj = future.get();
                                boolean first = i == 0;
                                if (first) {
                                    ret = obj;
                                }
                            }
                            return ret;
                        } else if (multiWriteType == MultiWriteType.SINGLE_THREAD) {
                            Object ret = null;
                            for (int i=0; i<writeOperation.getWriteResources().size(); i++) {
                                Resource resource = writeOperation.getWriteResources().get(i);
                                T client2 = clientMap.get(resource);
                                incrWrite(resource, method);
                                Object obj = method.invoke(client2, objects);
                                boolean first = i == 0;
                                if (first) {
                                    ret = obj;
                                }
                            }
                            return ret;
                        } else if (multiWriteType == MultiWriteType.ASYNC_MULTI_THREAD) {
                            ThreadContextSwitchStrategy strategy = env.getThreadContextSwitchStrategy();
                            Future<Object> targetFuture = null;
                            for (int i=0; i<writeOperation.getWriteResources().size(); i++) {
                                Resource resource = writeOperation.getWriteResources().get(i);
                                boolean first = i == 0;
                                T client2 = clientMap.get(resource);
                                incrWrite(resource, method);
                                Future<Object> future = null;
                                try {
                                    future = env.getMultiWriteAsyncExec().submit(String.valueOf(Thread.currentThread().getId()), strategy.wrapperCallable(() -> {
                                        try {
                                            incrWrite(resource, method);
                                            return method.invoke(client2, objects);
                                        } catch (Exception e) {
                                            logger.error("async multi thread invoke error, class = {}, method = {}, resource = {}",
                                                    className, method.getName(), resource.getUrl(), e);
                                            throw e;
                                        }
                                    }));
                                } catch (Exception e) {
                                    if (!first) {
                                        logger.error("submit async multi thread task error, class = {}, method = {}, resource = {}",
                                                className, method.getName(), resource.getUrl(), e);
                                    } else {
                                        throw e;
                                    }
                                }
                                if (first) {
                                    targetFuture = future;
                                }
                            }
                            if (targetFuture != null) {
                                return targetFuture.get();
                            } else {
                                throw new IllegalStateException("wil not invoke here");
                            }
                        } else {
                            throw new IllegalArgumentException("unknown multiWriteType");
                        }
                    default:
                        throw new IllegalArgumentException("unknown operation write type");
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

    private void incrWrite(Resource resource, Method method) {
        if (env != null && env.getMonitor() != null) {
            env.getMonitor().incrWrite(resource.getUrl(), className, readWriteOperationCache.getMethodName(method));
        }
    }

    private void incrRead(Resource resource, Method method) {
        if (env != null && env.getMonitor() != null) {
            env.getMonitor().incrRead(resource.getUrl(), className, readWriteOperationCache.getMethodName(method));
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
