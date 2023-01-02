package com.netease.nim.camellia.redis.proxy.enums;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ResourceUtil;
import com.netease.nim.camellia.redis.resource.RedisType;

import java.util.Set;

/**
 * Created by caojiajun on 2022/10/9
 */
public enum ProxyRouteType {
    REDIS_STANDALONE,//代理到redis-standalone
    REDIS_SENTINEL,//代理到redis-sentinel
    REDIS_CLUSTER,//代理到redis-cluster
    COMPLEX,//复杂路由规则，如读写分离、双写、分片等等
    ;

    public static ProxyRouteType fromResourceTable(ResourceTable resourceTable) {
        if (resourceTable == null) return COMPLEX;
        ResourceTable.Type type = resourceTable.getType();
        if (type != ResourceTable.Type.SIMPLE) return COMPLEX;
        Set<Resource> resources = ResourceUtil.getAllResources(resourceTable);
        if (resources.size() != 1) {
            return COMPLEX;
        }
        for (Resource resource : resources) {
            String url = resource.getUrl();
            if (url.startsWith(RedisType.Redis.getPrefix())) {
                return REDIS_STANDALONE;
            } else if (url.startsWith(RedisType.RedisSentinel.getPrefix())) {
                return REDIS_SENTINEL;
            } else if (url.startsWith(RedisType.RedisCluster.getPrefix())) {
                return REDIS_CLUSTER;
            }
        }
        return COMPLEX;
    }
}
