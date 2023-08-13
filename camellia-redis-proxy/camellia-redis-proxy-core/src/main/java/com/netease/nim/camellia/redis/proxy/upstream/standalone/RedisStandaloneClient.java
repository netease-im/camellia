package com.netease.nim.camellia.redis.proxy.upstream.standalone;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.resource.RedissResource;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.base.resource.RedisResource;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * Created by caojiajun on 2019/12/18.
 */
public class RedisStandaloneClient extends AbstractSimpleRedisClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisStandaloneClient.class);

    private final Resource resource;
    private final RedisConnectionAddr addr;

    public RedisStandaloneClient(RedisResource resource) {
        this.resource = resource;
        this.addr = new RedisConnectionAddr(resource.getHost(), resource.getPort(), resource.getUserName(), resource.getPassword(), resource.getDb());
    }

    public RedisStandaloneClient(RedissResource resource) {
        this.resource = resource;
        this.addr = new RedisConnectionAddr(resource.getHost(), resource.getPort(), resource.getUserName(), resource.getPassword(), resource.getDb());
    }

    @Override
    public void start() {
        logger.info("RedisStandaloneClient start success, resource = {}", PasswordMaskUtils.maskResource(getResource()));
    }

    @Override
    public RedisConnectionAddr getAddr() {
        return addr;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public boolean isValid() {
        RedisConnectionStatus status = getStatus(getAddr());
        return status == RedisConnectionStatus.VALID;
    }
}
