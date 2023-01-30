package com.netease.nim.camellia.redis.proxy.upstream.proxies;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.resource.RedisProxiesResource;
import com.netease.nim.camellia.redis.proxy.upstream.standalone.AsyncCamelliaSimpleClient;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClient;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientAddr;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientHub;
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
public class AsyncCameliaRedisProxiesClient extends AsyncCamelliaSimpleClient {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCameliaRedisProxiesClient.class);

    private final RedisProxiesResource redisProxiesResource;
    private final List<RedisClientAddr> list = new ArrayList<>();
    private List<RedisClientAddr> dynamicList;

    public AsyncCameliaRedisProxiesClient(RedisProxiesResource redisProxiesResource) {
        this.redisProxiesResource = redisProxiesResource;
        List<RedisProxiesResource.Node> nodes = redisProxiesResource.getNodes();
        for (RedisProxiesResource.Node node : nodes) {
            list.add(new RedisClientAddr(node.getHost(), node.getPort(), redisProxiesResource.getUserName(), redisProxiesResource.getPassword()));
        }
        dynamicList = new ArrayList<>(list);
        int seconds = ProxyDynamicConf.getInt("redis.proxies.reload.interval.seconds", 60);
        ExecutorUtils.scheduleAtFixedRate(this::reload, seconds, seconds, TimeUnit.SECONDS);
    }

    @Override
    public void preheat() {
        logger.info("try preheat, url = {}", PasswordMaskUtils.maskResource(getResource().getUrl()));
        for (RedisClientAddr addr : list) {
            logger.info("try preheat, url = {}, proxy = {}", PasswordMaskUtils.maskResource(getResource().getUrl()), PasswordMaskUtils.maskAddr(addr));
            boolean result = RedisClientHub.getInstance().preheat(addr.getHost(), addr.getPort(), addr.getUserName(), addr.getPassword());
            logger.info("preheat result = {}, url = {}, proxy = {}", result, PasswordMaskUtils.maskResource(getResource().getUrl()), PasswordMaskUtils.maskAddr(addr));
        }
        logger.info("preheat success, url = {}", PasswordMaskUtils.maskResource(getResource().getUrl()));
    }

    private void reload() {
        try {
            List<RedisClientAddr> dynamicList = new ArrayList<>();
            for (RedisClientAddr addr : list) {
                RedisClient redisClient = RedisClientHub.getInstance().get(addr);
                if (redisClient != null && redisClient.isValid()) {
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
    public RedisClientAddr getAddr() {
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
                RedisClientAddr addr = dynamicList.get(i);
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


    private boolean check(RedisClientAddr addr) {
        RedisClient redisClient = RedisClientHub.getInstance().get(addr);
        return redisClient != null && redisClient.isValid();
    }
}
