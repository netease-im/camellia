package com.netease.nim.camellia.redis.proxy.upstream.standalone;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.base.resource.RedisResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * Created by caojiajun on 2019/12/18.
 */
public class RedisStandaloneClient extends AbstractSimpleRedisClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisStandaloneClient.class);

    private final RedisResource redisResource;
    private final RedisConnectionAddr addr;

    public RedisStandaloneClient(RedisResource redisResource) {
        this.redisResource = redisResource;
        this.addr = new RedisConnectionAddr(redisResource.getHost(), redisResource.getPort(), redisResource.getUserName(), redisResource.getPassword(), redisResource.getDb());
        logger.info("RedisStandaloneClient init success, resource = {}", redisResource.getUrl());
    }

    @Override
    public RedisConnectionAddr getAddr() {
        return addr;
    }

    @Override
    public Resource getResource() {
        return redisResource;
    }

    @Override
    public boolean isValid() {
        return checkValid(getAddr());
    }
}
