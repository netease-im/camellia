package com.netease.nim.camellia.core.client.hub.standard;

import com.netease.nim.camellia.core.client.callback.OperationCallback;
import com.netease.nim.camellia.core.client.callback.ProxyClientFactory;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.hub.IProxyHub;
import com.netease.nim.camellia.core.client.hub.ShardingProxyHub;
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
public class StandardShardingProxyHub<T> implements IProxyHub<T> {

    private final ShardingProxyHub<T> shardingProxyHub;

    public StandardShardingProxyHub(Class<T> clazz, ResourceTable.ShardingTable shardingTable) {
        this(clazz, shardingTable, null);
    }

    public StandardShardingProxyHub(Class<T> clazz, ResourceTable.ShardingTable shardingTable, Resource defaultResource) {
        this(clazz, shardingTable, defaultResource, null);
    }

    public StandardShardingProxyHub(Class<T> clazz, ResourceTable.ShardingTable shardingTable, Resource defaultResource, ProxyEnv env) {
        if (!CheckUtil.checkShardingTable(shardingTable)) {
            throw new IllegalArgumentException("shardingTable check fail");
        }
        Set<Resource> allResource = ResourceUtil.getAllResources(shardingTable);
        try {
            Map<Resource, T> clientMap = new HashMap<>();
            for (Resource resource : allResource) {
                T client = clazz.getConstructor(Resource.class).newInstance(resource);
                clientMap.put(resource, client);
            }
            Map<ResourceOperation, T> map = new HashMap<>();
            Map<Integer, T> proxyMap = new HashMap<>();
            for (Map.Entry<Integer, ResourceOperation> entry : shardingTable.getResourceOperationMap().entrySet()) {
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
            int bucketSize = shardingTable.getBucketSize();
            this.shardingProxyHub = new ShardingProxyHub<>(bucketSize, proxyMap, env);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public T chooseProxy(byte[]... key) {
        return shardingProxyHub.chooseProxy(key);
    }
}
