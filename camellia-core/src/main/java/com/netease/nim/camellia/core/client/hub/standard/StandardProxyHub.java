package com.netease.nim.camellia.core.client.hub.standard;


import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.hub.IProxyHub;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.CheckUtil;

/**
 *
 * Created by caojiajun on 2019/5/16.
 */
public class StandardProxyHub<T> implements IProxyHub<T> {

    private IProxyHub<T> proxyHub;

    public StandardProxyHub(Class<T> clazz, ResourceTable resourceTable) {
        this(clazz, resourceTable, null);
    }

    public StandardProxyHub(Class<T> clazz, ResourceTable resourceTable, Resource defaultResource) {
        this(clazz, resourceTable, defaultResource, null);
    }

    public StandardProxyHub(Class<T> clazz, ResourceTable resourceTable, Resource defaultResource, ProxyEnv env) {
        if (!CheckUtil.checkResourceTable(resourceTable)) {
            throw new IllegalArgumentException("resourceTable check fail");
        }
        ResourceTable.Type type = resourceTable.getType();
        IProxyHub<T> proxyHub;
        switch (type) {
            case SHADING:
                proxyHub = new StandardShadingProxyHub<>(clazz, resourceTable.getShadingTable(), defaultResource, env);
                break;
            case SIMPLE:
                proxyHub = new StandardSimpleProxyHub<>(clazz, resourceTable.getSimpleTable(), defaultResource, env);
                break;
            default:
                throw new IllegalArgumentException("ResourceTable type error");
        }
        this.proxyHub = proxyHub;
    }

    @Override
    public T chooseProxy(byte[]... key) {
        return proxyHub.chooseProxy(key);
    }
}
