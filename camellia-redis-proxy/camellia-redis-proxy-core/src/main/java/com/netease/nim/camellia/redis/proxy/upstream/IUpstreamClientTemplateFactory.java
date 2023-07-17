package com.netease.nim.camellia.redis.proxy.upstream;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/1/31
 */
public interface IUpstreamClientTemplateFactory {

    /**
     * get a IUpstreamClientTemplate instance
     * in sync, this method maybe blocking, if isMultiTenantsSupport=false, it should no-blocking
     * @param bid bid
     * @param bgroup bgroup
     * @return instance
     */
    IUpstreamClientTemplate getOrInitialize(Long bid, String bgroup);

    /**
     * get or async initialize a IUpstreamClientTemplate instance
     * @param bid bid
     * @param bgroup bgroup
     * @return instance
     */
    CompletableFuture<IUpstreamClientTemplate> getOrInitializeAsync(Long bid, String bgroup);

    /**
     * try get initialized instance from cache, no-blocking
     * @param bid bid
     * @param bgroup bgroup
     * @return instance
     */
    IUpstreamClientTemplate tryGet(Long bid, String bgroup);

    /**
     * is multi tenants support
     * @return true/false
     */
    boolean isMultiTenantsSupport();

    /**
     * get RedisProxyEnv
     * @return RedisProxyEnv
     */
    default RedisProxyEnv getEnv() {
        return null;
    }

    /**
     * get all IUpstreamClientTemplate instances
     * @return instances
     */
    List<IUpstreamClientTemplate> getAll();

    static CompletableFuture<IUpstreamClientTemplate> wrapper(IUpstreamClientTemplate template) {
        CompletableFuture<IUpstreamClientTemplate> future = new CompletableFuture<>();
        future.complete(template);
        return future;
    }
}
