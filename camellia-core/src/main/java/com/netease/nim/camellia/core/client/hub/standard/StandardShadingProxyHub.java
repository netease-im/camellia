package com.netease.nim.camellia.core.client.hub.standard;

import com.netease.nim.camellia.core.client.callback.OperationCallback;
import com.netease.nim.camellia.core.client.callback.ProxyClientFactory;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.hub.IProxyHub;
import com.netease.nim.camellia.core.client.hub.ShadingProxyHub;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.util.CheckUtil;
import com.netease.nim.camellia.core.util.ResourceUtil;

import java.util.*;

/**
 *
 * Created by caojiajun on 2019/5/16.
 */
public class StandardShadingProxyHub<T> implements IProxyHub<T> {

    private ShadingProxyHub<T> shadingProxyHub;

    public StandardShadingProxyHub(Class<T> clazz, ResourceTable.ShadingTable shadingTable) {
        this(clazz, shadingTable, null);
    }

    public StandardShadingProxyHub(Class<T> clazz, ResourceTable.ShadingTable shadingTable, Resource defaultResource) {
        this(clazz, shadingTable, defaultResource, null);
    }

    public StandardShadingProxyHub(Class<T> clazz, ResourceTable.ShadingTable shadingTable, Resource defaultResource, ProxyEnv env) {
        if (!CheckUtil.checkShadingTable(shadingTable)) {
            throw new IllegalArgumentException("shadingTable check fail");
        }
        Set<Resource> allResource = ResourceUtil.getAllResources(shadingTable);
        try {
            Map<Resource, T> clientMap = new HashMap<>();
            for (Resource resource : allResource) {
                T client = clazz.getConstructor(Resource.class).newInstance(resource);
                clientMap.put(resource, client);
            }
            Map<ResourceOperation, T> map = new HashMap<>();
            Map<Integer, T> proxyMap = new HashMap<>();
            for (Map.Entry<Integer, ResourceOperation> entry : shadingTable.getResourceOperationMap().entrySet()) {
                Integer index = entry.getKey();
                ResourceOperation resourceOperation = entry.getValue();
                T proxy = map.get(resourceOperation);
                if (proxy == null) {
                    proxy = ProxyClientFactory.createProxy(clazz, new Class[]{Resource.class}, new Object[]{defaultResource},
                            new OperationCallback<>(resourceOperation, clientMap, clazz, env));
                }
                proxyMap.put(index, proxy);
                map.put(resourceOperation, proxy);
            }
            int bucketSize = shadingTable.getBucketSize();
            this.shadingProxyHub = new ShadingProxyHub<>(bucketSize, proxyMap, env);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public T chooseProxy(byte[]... key) {
        return shadingProxyHub.chooseProxy(key);
    }
}
