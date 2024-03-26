package com.netease.nim.camellia.core.client.env;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;
import com.netease.nim.camellia.core.util.ReadWriteOperationCache;

import java.lang.reflect.Method;

/**
 * Created by caojiajun on 2024/3/25
 */
public class FailedWriteTask {

    private final ResourceOperation.Type type;
    private final ResourceWriteOperation.Type writeType;
    private final FailedReason failedReason;
    private final int index;//0表示第一个写地址，1表示第2个写地址，依次类推
    private final Resource resource;
    private final Object client;
    private final Method method;
    private final Object[] objects;
    private final ProxyEnv proxyEnv;
    private final String className;
    private final ReadWriteOperationCache readWriteOperationCache;

    public FailedWriteTask(ResourceOperation.Type type, ResourceWriteOperation.Type writeType, FailedReason failedReason, int index, ProxyEnv proxyEnv, Resource resource, Object client, String className, Method method,
                           Object[] objects, ReadWriteOperationCache readWriteOperationCache) {
        this.type = type;
        this.writeType = writeType;
        this.failedReason = failedReason;
        this.index = index;
        this.proxyEnv = proxyEnv;
        this.resource = resource;
        this.client = client;
        this.className = className;
        this.method = method;
        this.objects = objects;
        this.readWriteOperationCache = readWriteOperationCache;
    }

    /**
     * 执行本方法进行重试
     * @return 执行结果
     * @throws Exception 异常
     */
    public Object invoke() throws Exception {
        incrWrite(resource, method);
        return method.invoke(client, objects);
    }

    public int getIndex() {
        return index;
    }

    public ResourceOperation.Type getType() {
        return type;
    }

    public ResourceWriteOperation.Type getWriteType() {
        return writeType;
    }

    public FailedReason getFailedReason() {
        return failedReason;
    }

    public Resource getResource() {
        return resource;
    }

    public Object getClient() {
        return client;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getObjects() {
        return objects;
    }

    public ProxyEnv getProxyEnv() {
        return proxyEnv;
    }

    public MultiWriteType getMultiWriteType() {
        return proxyEnv.getMultiWriteType();
    }

    private void incrWrite(Resource resource, Method method) {
        if (proxyEnv != null && proxyEnv.getMonitor() != null) {
            proxyEnv.getMonitor().incrWrite(resource.getUrl(), className, readWriteOperationCache.getMethodName(method));
        }
    }

}
