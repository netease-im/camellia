package com.netease.nim.camellia.redis.proxy.upstream.proxies;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.resource.RedisProxiesResource;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于固定配置的proxy client
 * 对于每个proxy，都会当做一个普通的redis去访问
 */
public class RedisProxiesClient extends AbstractRedisProxiesClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisProxiesClient.class);

    private final RedisProxiesResource resource;
    private final List<RedisConnectionAddr> list = new ArrayList<>();

    public RedisProxiesClient(RedisProxiesResource resource) {
        this.resource = resource;
        List<RedisProxiesResource.Node> nodes = resource.getNodes();
        for (RedisProxiesResource.Node node : nodes) {
            list.add(new RedisConnectionAddr(node.getHost(), node.getPort(), resource.getUserName(), resource.getPassword(), resource.getDb()));
        }
        init();
        logger.info("RedisProxiesClient init success, resource = {}", resource.getUrl());
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public List<RedisConnectionAddr> getAll() {
        return new ArrayList<>(list);
    }
}
