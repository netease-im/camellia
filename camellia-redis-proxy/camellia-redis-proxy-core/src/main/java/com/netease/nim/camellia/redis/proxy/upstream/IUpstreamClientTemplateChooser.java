package com.netease.nim.camellia.redis.proxy.upstream;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/1/31
 */
public interface IUpstreamClientTemplateChooser {

    /**
     * choose a IUpstreamClientTemplate instance
     * in sync, this method maybe blocking, if isMultiTenancySupport=false, it should no-blocking
     * @param bid bid
     * @param bgroup bgroup
     * @return instance
     */
    IUpstreamClientTemplate choose(Long bid, String bgroup);

    /**
     * async choose a IUpstreamClientTemplate instance
     * @param bid bid
     * @param bgroup bgroup
     * @return instance
     */
    CompletableFuture<IUpstreamClientTemplate> chooseAsync(Long bid, String bgroup);

    /**
     * isMultiTenancySupport
     * @return true/false
     */
    boolean isMultiTenancySupport();

    /**
     * get RedisProxyEnv
     * @return RedisProxyEnv
     */
    default RedisProxyEnv getEnv() {
        return null;
    }
}
