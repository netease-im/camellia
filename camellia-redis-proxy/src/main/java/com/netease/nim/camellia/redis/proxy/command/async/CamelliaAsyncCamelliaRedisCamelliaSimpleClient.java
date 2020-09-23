package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.resource.RedisResource;

/**
 *
 * Created by caojiajun on 2019/12/18.
 */
public class CamelliaAsyncCamelliaRedisCamelliaSimpleClient extends AsyncCamelliaSimpleClient {

    private final RedisResource redisResource;
    private final RedisClientAddr addr;

    public CamelliaAsyncCamelliaRedisCamelliaSimpleClient(RedisResource redisResource) {
        this.redisResource = redisResource;
        this.addr = new RedisClientAddr(redisResource.getHost(), redisResource.getPort(), redisResource.getPassword());
    }

    @Override
    public RedisClientAddr getAddr() {
        return addr;
    }

    @Override
    public Resource getResource() {
        return redisResource;
    }
}
