package com.netease.nim.camellia.redis.proxy.upstream.standalone;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.base.resource.RedisResource;

/**
 *
 * Created by caojiajun on 2019/12/18.
 */
public class RedisStandaloneClient extends AbstractSimpleRedisClient {

    private final RedisResource redisResource;
    private final RedisConnectionAddr addr;

    public RedisStandaloneClient(RedisResource redisResource) {
        this.redisResource = redisResource;
        this.addr = new RedisConnectionAddr(redisResource.getHost(), redisResource.getPort(), redisResource.getUserName(), redisResource.getPassword(), redisResource.getDb());
    }

    @Override
    public RedisConnectionAddr getAddr() {
        return addr;
    }

    @Override
    public Resource getResource() {
        return redisResource;
    }
}
