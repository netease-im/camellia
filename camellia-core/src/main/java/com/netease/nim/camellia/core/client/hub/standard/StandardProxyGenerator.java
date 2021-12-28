package com.netease.nim.camellia.core.client.hub.standard;

import com.netease.nim.camellia.core.client.callback.ProxyClientFactory;
import com.netease.nim.camellia.core.client.callback.ShardingCallback;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.hub.IProxyHub;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.CheckUtil;


/**
 *
 * Created by caojiajun on 2019/5/17.
 */
public class StandardProxyGenerator<T> {

    private Class<T> clazz;
    private ResourceTable resourceTable;
    private Resource defaultResource;
    private IProxyHub<T> proxyHub;
    private ProxyEnv env;

    public StandardProxyGenerator(Class<T> clazz, ResourceTable resourceTable) {
        this(clazz, resourceTable, null);
    }

    public StandardProxyGenerator(Class<T> clazz, ResourceTable resourceTable, Resource defaultResource) {
        this(clazz, resourceTable, defaultResource, null);
    }

    public StandardProxyGenerator(Class<T> clazz, ResourceTable resourceTable, Resource defaultResource, ProxyEnv env) {
        this.clazz = clazz;
        this.resourceTable = resourceTable;
        this.defaultResource = defaultResource;
        this.env = env;
    }

    public T generate() {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz is null");
        }
        if (!CheckUtil.checkResourceTable(resourceTable)) {
            throw new IllegalArgumentException("resourceTable check fail");
        }
        StandardProxyHub<T> standardProxyHub = new StandardProxyHub<>(clazz, resourceTable, defaultResource, env);
        this.proxyHub = standardProxyHub;
        switch (resourceTable.getType()) {
            case SIMPLE:
                //随便返回一个即可，因为只有一个
                return standardProxyHub.chooseProxy(new byte[0]);
            case SHARDING:
                //此时有多个，需要根据sharding规则动态返回，此时再生成一个代理类，封装掉sharding逻辑
                return ProxyClientFactory.createProxy(clazz, new Class[] {Resource.class}, new Object[] {null},
                        new ShardingCallback<>(standardProxyHub, clazz, env));
            default:
                throw new IllegalArgumentException("ResourceTable type error");
        }
    }

    public IProxyHub<T> getProxyHub() {
        return proxyHub;
    }

}
