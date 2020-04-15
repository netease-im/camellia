package com.netease.nim.camellia.core.client.hub.standard;


import com.netease.nim.camellia.core.client.callback.OperationCallback;
import com.netease.nim.camellia.core.client.callback.ProxyClientFactory;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.hub.IProxyHub;
import com.netease.nim.camellia.core.client.hub.SimpleProxyHub;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.CheckUtil;
import com.netease.nim.camellia.core.util.ResourceUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2019/5/16.
 */
public class StandardSimpleProxyHub<T> implements IProxyHub<T> {

    private SimpleProxyHub<T> proxyHub;

    public StandardSimpleProxyHub(Class<T> clazz, ResourceTable.SimpleTable simpleTable) {
        this(clazz, simpleTable, null);
    }

    public StandardSimpleProxyHub(Class<T> clazz, ResourceTable.SimpleTable simpleTable, Resource defaultResource) {
        this(clazz, simpleTable, defaultResource, null);
    }

    public StandardSimpleProxyHub(Class<T> clazz, ResourceTable.SimpleTable simpleTable, Resource defaultResource, ProxyEnv env) {
        if (!CheckUtil.checkSimpleTable(simpleTable)) {
            throw new IllegalArgumentException("simpleTable check fail");
        }
        Set<Resource> allResource = ResourceUtil.getAllResources(simpleTable);
        Map<Resource, T> clientMap = new HashMap<>();
        try {
            for (Resource resource : allResource) {
                T client = clazz.getConstructor(Resource.class).newInstance(resource);
                clientMap.put(resource, client);
            }
            T proxy = ProxyClientFactory.createProxy(clazz, new Class[]{Resource.class}, new Object[]{defaultResource},
                    new OperationCallback<>(simpleTable.getResourceOperation(), clientMap, clazz, env));
            this.proxyHub = new SimpleProxyHub<>(proxy);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public T chooseProxy(byte[]... key) {
        return proxyHub.chooseProxy(key);
    }

}
