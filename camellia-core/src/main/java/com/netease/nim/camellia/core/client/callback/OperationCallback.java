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
                        //
                        Resource resource = writeOperation.getWriteResource();
                        try {
                            return invokeWrite(resource, method, objects);
                        } catch (Exception e) {
                            T client = clientMap.get(resource);
                            addFailedWriteTask(type, writeOperation.getType(), FailedReason.EXCEPTION, e, 0, resource, client, objects, method);
                            throw e;
                        }
                    }
                    case MULTI: {
                        //
                        List<Resource> writeResources = writeOperation.getWriteResources();
                        //
                        return MultiWriteInvoker.invoke(env, writeResources, (resource, index) -> invokeWrite(resource, method, objects), (err, resource, index, failedReason) -> {
                            //failed callback
                            T client = clientMap.get(resource);
                            addFailedWriteTask(type, writeOperation.getType(), failedReason, err, index, resource, client, objects, method);
                        });
                    }
                    default: {
                        throw new IllegalArgumentException("unknown operation write type");
                    }
                }
            }
            default: {
                throw new IllegalArgumentException("unknown operation type");
            }
        }
    }

    private Object invokeWrite(Resource resource, Method method, Object[] objects) throws Exception {
        //invoke logic
        T client = clientMap.get(resource);
        incrWrite(resource, method);
        return method.invoke(client, objects);
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
                    default: {
                        throw new RuntimeException("unknown operation read type");
                    }
                }
            }
            default: {
                throw new IllegalArgumentException("unknown operation type");
            }
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
                    default: {
                        throw new IllegalArgumentException("resourceOperation/clientMap not match");
                    }
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
                    default: {
                        throw new IllegalArgumentException("resourceOperation/clientMap not match");
                    }
                }
        }
    }
}
