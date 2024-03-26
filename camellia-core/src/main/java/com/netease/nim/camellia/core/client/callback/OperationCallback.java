package com.netease.nim.camellia.core.client.callback;

import com.netease.nim.camellia.core.client.env.*;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceReadOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;
import com.netease.nim.camellia.core.util.CheckUtil;
import com.netease.nim.camellia.tools.utils.ExceptionUtils;
import com.netease.nim.camellia.core.util.ReadWriteOperationCache;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private void addFailedWriteTask(ResourceOperation.Type type, ResourceWriteOperation.Type writeType,
                                    FailedReason failedReason, Throwable error, int index, Resource resource, Object client, Object[] objects, Method method) {
        FailedWriteTaskQueue queue = env.getFailedWriteTaskQueue();
        if (queue != null) {
            FailedWriteTask task = new FailedWriteTask(type, writeType, failedReason, error, index, env,
                    resource, client, className, method, objects, readWriteOperationCache);
            queue.offerQueue(task);
        }
    }

    private Object write(final Object[] objects, final Method method) throws Throwable {
        ResourceOperation.Type type = resourceOperation.getType();
        switch (type) {
            case SIMPLE: {
                Resource resource = resourceOperation.getResource();
                T client = clientMap.get(resource);
                try {
                    incrWrite(resource, method);
                    return method.invoke(client, objects);
                } catch (Throwable e) {
                    addFailedWriteTask(type, null, FailedReason.EXCEPTION, e, 0, resource, client, objects, method);
                    throw e;
                }
            }
            case RW_SEPARATE: {
                ResourceWriteOperation writeOperation = resourceOperation.getWriteOperation();
                switch (writeOperation.getType()) {
                    case SIMPLE: {
                        Resource resource = writeOperation.getWriteResource();
                        T client = clientMap.get(resource);
                        try {
                            incrWrite(resource, method);
                            return method.invoke(client, objects);
                        } catch (Throwable e) {
                            addFailedWriteTask(type, writeOperation.getType(), FailedReason.EXCEPTION, e, 0, resource, client, objects, method);
                            throw e;
                        }
                    }
                    case MULTI: {
                        //只有1个写地址
                        List<Resource> writeResources = writeOperation.getWriteResources();
                        if (writeResources.size() == 1) {
                            Resource resource = writeResources.get(0);
                            T client = clientMap.get(resource);
                            try {
                                incrWrite(resource, method);
                                return method.invoke(client, objects);
                            } catch (Throwable e) {
                                addFailedWriteTask(type, writeOperation.getType(), FailedReason.EXCEPTION, e, 0, resource, client, objects, method);
                                throw e;
                            }
                        }
                        //有多个写地址
                        MultiWriteType multiWriteType = env.getMultiWriteType();
                        if (multiWriteType == MultiWriteType.MULTI_THREAD_CONCURRENT) {//多线程并发
                            ThreadContextSwitchStrategy strategy = env.getThreadContextSwitchStrategy();
                            List<Future<Object>> futureList = new ArrayList<>();
                            for (int i = 0; i < writeOperation.getWriteResources().size(); i++) {
                                Resource resource = writeOperation.getWriteResources().get(i);
                                final T client = clientMap.get(resource);
                                final AtomicBoolean exception = new AtomicBoolean(false);
                                final int index = i;
                                Future<Object> future;
                                try {
                                    future = env.getMultiWriteConcurrentExec().submit(strategy.wrapperCallable(() -> {
                                        try {
                                            incrWrite(resource, method);
                                            return method.invoke(client, objects);
                                        } catch (Throwable e) {
                                            logger.error("multi thread concurrent invoke error, class = {}, method = {}, resource = {}",
                                                    className, method.getName(), resource.getUrl(), e);
                                            addFailedWriteTask(type, writeOperation.getType(), FailedReason.EXCEPTION, e, index, resource, client, objects, method);
                                            exception.set(true);
                                            throw e;
                                        }
                                    }));
                                } catch (Throwable e) {
                                    if (!exception.get()) {
                                        addFailedWriteTask(type, writeOperation.getType(), FailedReason.DISCARD, e, index, resource, client, objects, method);
                                    }
                                    throw e;
                                }
                                futureList.add(future);
                            }
                            Object ret = null;
                            for (int i = 0; i < futureList.size(); i++) {
                                Future<Object> future = futureList.get(i);
                                Object obj = future.get();
                                boolean first = i == 0;
                                if (first) {
                                    ret = obj;
                                }
                            }
                            return ret;
                        } else if (multiWriteType == MultiWriteType.SINGLE_THREAD) {//单线程
                            Object ret = null;
                            for (int i = 0; i < writeOperation.getWriteResources().size(); i++) {
                                Resource resource = writeOperation.getWriteResources().get(i);
                                T client = clientMap.get(resource);
                                try {
                                    incrWrite(resource, method);
                                    Object obj = method.invoke(client, objects);
                                    boolean first = i == 0;
                                    if (first) {
                                        ret = obj;
                                    }
                                } catch (Throwable e) {
                                    addFailedWriteTask(type, writeOperation.getType(), FailedReason.EXCEPTION, e, i, resource, client, objects, method);
                                    throw e;
                                }
                            }
                            return ret;
                        } else if (multiWriteType == MultiWriteType.ASYNC_MULTI_THREAD) {
                            ThreadContextSwitchStrategy strategy = env.getThreadContextSwitchStrategy();
                            Future<Object> targetFuture = null;
                            for (int i = 0; i < writeOperation.getWriteResources().size(); i++) {
                                Resource resource = writeOperation.getWriteResources().get(i);
                                boolean first = i == 0;
                                T client = clientMap.get(resource);
                                final AtomicBoolean exception = new AtomicBoolean(false);
                                final int index = i;
                                Future<Object> future = null;
                                try {
                                    future = env.getMultiWriteAsyncExec().submit(String.valueOf(Thread.currentThread().getId()), strategy.wrapperCallable(() -> {
                                        try {
                                            incrWrite(resource, method);
                                            return method.invoke(client, objects);
                                        } catch (Throwable e) {
                                            logger.error("async multi thread invoke error, class = {}, method = {}, resource = {}",
                                                    className, method.getName(), resource.getUrl(), e);
                                            addFailedWriteTask(type, writeOperation.getType(), FailedReason.EXCEPTION, e, index, resource, client, objects, method);
                                            exception.set(true);
                                            throw e;
                                        }
                                    }));
                                } catch (Throwable e) {
                                    if (!exception.get()) {
                                        addFailedWriteTask(type, writeOperation.getType(), FailedReason.DISCARD, e, index, resource, client, objects, method);
                                    }
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
                        } else if (multiWriteType == MultiWriteType.MISC_ASYNC_MULTI_THREAD) {
                            ThreadContextSwitchStrategy strategy = env.getThreadContextSwitchStrategy();
                            Object target = null;
                            for (int i = 0; i < writeOperation.getWriteResources().size(); i++) {
                                Resource resource = writeOperation.getWriteResources().get(i);
                                boolean first = i == 0;
                                final AtomicBoolean exception = new AtomicBoolean(false);
                                final int index = i;
                                T client = clientMap.get(resource);
                                if (first) {
                                    try {
                                        incrWrite(resource, method);
                                        target = method.invoke(client, objects);
                                    } catch (Throwable e) {
                                        addFailedWriteTask(type, writeOperation.getType(), FailedReason.EXCEPTION, e, index, resource, client, objects, method);
                                        exception.set(true);
                                        throw e;
                                    }
                                } else {
                                    try {
                                        env.getMultiWriteAsyncExec().submit(String.valueOf(Thread.currentThread().getId()), strategy.wrapperCallable(() -> {
                                            try {
                                                incrWrite(resource, method);
                                                return method.invoke(client, objects);
                                            } catch (Throwable e) {
                                                logger.error("async multi thread invoke error, class = {}, method = {}, resource = {}",
                                                        className, method.getName(), resource.getUrl(), e);
                                                addFailedWriteTask(type, writeOperation.getType(), FailedReason.EXCEPTION, e, index, resource, client, objects, method);
                                                exception.set(true);
                                                throw e;
                                            }
                                        }));
                                    } catch (Throwable e) {
                                        logger.error("submit async multi thread task error, class = {}, method = {}, resource = {}",
                                                className, method.getName(), resource.getUrl(), e);
                                        if (!exception.get()) {
                                            addFailedWriteTask(type, writeOperation.getType(), FailedReason.DISCARD, e, index, resource, client, objects, method);
                                        }
                                    }
                                }
                            }
                            return target;
                        } else {
                            throw new IllegalArgumentException("unknown multiWriteType");
                        }
                    }
                    default:
                        throw new IllegalArgumentException("unknown operation write type");
                }
            }
            default:
                throw new IllegalArgumentException("unknown operation type");
        }
    }

    private Object read(Object[] objects, Method method) throws Throwable {
        ResourceOperation.Type type = resourceOperation.getType();
        switch (type) {
            case SIMPLE: {
                Resource resource = resourceOperation.getResource();
                T client = clientMap.get(resource);
                incrRead(resource, method);
                return method.invoke(client, objects);
            }
            case RW_SEPARATE: {
                ResourceReadOperation readOperation = resourceOperation.getReadOperation();
                switch (readOperation.getType()) {
                    case SIMPLE: {
                        Resource resource = readOperation.getReadResource();
                        T client = clientMap.get(resource);
                        incrRead(resource, method);
                        return method.invoke(client, objects);
                    }
                    case ORDER: {
                        Throwable ex = null;
                        for (Resource resource : readOperation.getReadResources()) {
                            T client = clientMap.get(resource);
                            try {
                                incrRead(resource, method);
                                return method.invoke(client, objects);
                            } catch (Throwable throwable) {
                                ex = throwable;
                            }
                        }
                        if (ex != null) {
                            throw ex;
                        }
                        throw new RuntimeException("no reachable read client");
                    }
                    case RANDOM: {
                        List<Resource> list = readOperation.getReadResources();
                        int index = ThreadLocalRandom.current().nextInt(list.size());
                        Resource resource = list.get(index);
                        T client = clientMap.get(resource);
                        incrRead(resource, method);
                        return method.invoke(client, objects);
                    }
                    default:
                        throw new RuntimeException("unknown operation read type");
                }
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
