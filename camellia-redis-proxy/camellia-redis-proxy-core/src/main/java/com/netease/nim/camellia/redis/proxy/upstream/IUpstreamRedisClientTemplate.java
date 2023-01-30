package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.core.model.ResourceTable;

/**
 *
 * Created by caojiajun on 2019/12/19.
 */
public interface IUpstreamRedisClientTemplate extends IUpstreamClientTemplate {

    /**
     * 获取路由表
     * @return ResourceTable
     */
    ResourceTable getResourceTable();

    /**
     * 获取路由表更新时间
     * @return update time
     */
    long getResourceTableUpdateTime();

    /**
     * 预热，会预先建立好到后端redis的连接
     */
    void preheat();

    /**
     * 关闭
     */
    void shutdown();

    /**
     * 更新路由表
     * @param resourceTable ResourceTable
     */
    void update(ResourceTable resourceTable);
}
