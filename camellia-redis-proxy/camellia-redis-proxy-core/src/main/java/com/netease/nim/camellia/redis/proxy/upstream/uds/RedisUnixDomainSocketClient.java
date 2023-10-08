package com.netease.nim.camellia.redis.proxy.upstream.uds;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.resource.RedisUnixDomainSocketResource;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionStatus;
import com.netease.nim.camellia.redis.proxy.upstream.standalone.AbstractSimpleRedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2023/10/8
 */
public class RedisUnixDomainSocketClient extends AbstractSimpleRedisClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisUnixDomainSocketClient.class);

    private final Resource resource;
    private final RedisConnectionAddr addr;

    public RedisUnixDomainSocketClient(RedisUnixDomainSocketResource resource) {
        this.resource = resource;
        this.addr = new RedisConnectionAddr(resource.getUdsPath(), resource.getUserName(),
                resource.getPassword(), false, resource.getDb(), true);
    }

    @Override
    public void start() {
        logger.info("RedisUnixDomainSocketClient start success, resource = {}", PasswordMaskUtils.maskResource(getResource()));
    }

    @Override
    public boolean isValid() {
        RedisConnectionStatus status = getStatus(getAddr());
        return status == RedisConnectionStatus.VALID;
    }

    @Override
    public RedisConnectionAddr getAddr() {
        return addr;
    }

    @Override
    public Resource getResource() {
        return resource;
    }
}
