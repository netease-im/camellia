package com.netease.nim.camellia.redis.proxy.upstream.proxies;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.resource.RedisProxiesResource;
import com.netease.nim.camellia.redis.proxy.upstream.standalone.AbstractSimpleRedisClient;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 基于固定配置的proxy client
 * 对于每个proxy，都会当做一个普通的redis去访问
 */
public class RedisProxiesClient extends AbstractSimpleRedisClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisProxiesClient.class);

    private final RedisProxiesResource redisProxiesResource;
    private final List<RedisConnectionAddr> list = new ArrayList<>();
    private List<RedisConnectionAddr> dynamicList;

    public RedisProxiesClient(RedisProxiesResource redisProxiesResource) {
        this.redisProxiesResource = redisProxiesResource;
        List<RedisProxiesResource.Node> nodes = redisProxiesResource.getNodes();
        for (RedisProxiesResource.Node node : nodes) {
            list.add(new RedisConnectionAddr(node.getHost(), node.getPort(), redisProxiesResource.getUserName(), redisProxiesResource.getPassword()));
        }
        dynamicList = new ArrayList<>(list);
        int seconds = ProxyDynamicConf.getInt("redis.proxies.reload.interval.seconds", 60);
        ExecutorUtils.scheduleAtFixedRate(this::reload, seconds, seconds, TimeUnit.SECONDS);
        logger.info("RedisProxiesClient init success, resource = {}", redisProxiesResource.getUrl());
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
    public boolean isValid() {
        for (RedisConnectionAddr addr : list) {
            if (check(addr)) {
                return true;
            }
        }
        return false;
    }

    private void reload() {
        try {
            List<RedisConnectionAddr> dynamicList = new ArrayList<>();
            for (RedisConnectionAddr addr : list) {
                RedisConnection redisConnection = RedisConnectionHub.getInstance().get(addr);
                if (redisConnection != null && redisConnection.isValid()) {
                    dynamicList.add(addr);
                }
            }
            if (dynamicList.isEmpty()) {
                this.dynamicList = new ArrayList<>(list);
            } else {
                this.dynamicList = dynamicList;
            }
        } catch (Exception e) {
            logger.error("redis-proxies reload error", e);
        }
    }

    @Override
    public RedisConnectionAddr getAddr() {
        try {
            if (list.isEmpty()) return null;
            if (list.size() == 1) {
                return list.get(0);
            }
            int retry = list.size();
            while (retry-- > 0) {
                if (dynamicList.isEmpty()) {
                    dynamicList = new ArrayList<>(list);
                }
                int i = ThreadLocalRandom.current().nextInt(dynamicList.size());
                RedisConnectionAddr addr = dynamicList.get(i);
                if (check(addr)) {
                    return addr;
                } else {
                    dynamicList.remove(addr);
                }
            }
            int i = ThreadLocalRandom.current().nextInt(list.size());
            return list.get(i);
        } catch (Exception e) {
            try {
                if (list.isEmpty()) return null;
                int i = ThreadLocalRandom.current().nextInt(list.size());
                return list.get(i);
            } catch (Exception ex) {
                try {
                    return list.get(0);
                } catch (Exception exc) {
                    return null;
                }
            }
        }
    }

    @Override
    public Resource getResource() {
        return redisProxiesResource;
    }

}
