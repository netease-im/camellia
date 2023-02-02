package com.netease.nim.camellia.redis.proxy.upstream.proxies;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.resource.RedisProxiesResource;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
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
            list.add(new RedisConnectionAddr(node.getHost(), node.getPort(), resource.getUserName(), resource.getPassword()));
        }
        init();
        logger.info("RedisProxiesClient init success, resource = {}", resource.getUrl());
    }

    @Override
    public void preheat() {
        logger.info("try preheat, url = {}", PasswordMaskUtils.maskResource(getResource().getUrl()));
        for (RedisConnectionAddr addr : list) {
            logger.info("try preheat, url = {}, proxy = {}", PasswordMaskUtils.maskResource(getResource().getUrl()), PasswordMaskUtils.maskAddr(addr));
            boolean result = RedisConnectionHub.getInstance().preheat(addr.getHost(), addr.getPort(), addr.getUserName(), addr.getPassword());
            logger.info("preheat result = {}, url = {}, proxy = {}", result, PasswordMaskUtils.maskResource(getResource().getUrl()), PasswordMaskUtils.maskAddr(addr));
        }
        logger.info("preheat success, url = {}", PasswordMaskUtils.maskResource(getResource().getUrl()));
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
