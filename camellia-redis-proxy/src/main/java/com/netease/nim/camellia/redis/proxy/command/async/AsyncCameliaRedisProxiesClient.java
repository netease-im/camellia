package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.SysUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.resource.RedisProxiesResource;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class AsyncCameliaRedisProxiesClient extends AsyncCamelliaSimpleClient {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCameliaRedisProxiesClient.class);

    private static final ScheduledExecutorService scheduleService = Executors.newScheduledThreadPool(SysUtils.getCpuNum(), new DefaultThreadFactory("camellia-redis-proxies-reload"));

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
        scheduleService.scheduleAtFixedRate(this::reload, seconds, seconds, TimeUnit.SECONDS);
    }

    @Override
    public void preheat() {
        logger.info("try preheat, url = {}", PasswordMaskUtils.maskResource(getResource().getUrl()));
        for (RedisClientAddr addr : list) {
            logger.info("try preheat, url = {}, proxy = {}", PasswordMaskUtils.maskResource(getResource().getUrl()), PasswordMaskUtils.maskAddr(addr));
            boolean result = RedisClientHub.preheat(addr.getHost(), addr.getPort(), addr.getUserName(), addr.getPassword());
            logger.info("preheat result = {}, url = {}, proxy = {}", result, PasswordMaskUtils.maskResource(getResource().getUrl()), PasswordMaskUtils.maskAddr(addr));
        }
        logger.info("preheat success, url = {}", PasswordMaskUtils.maskResource(getResource().getUrl()));
    }

    private void reload() {
        try {
            List<RedisClientAddr> dynamicList = new ArrayList<>();
            for (RedisClientAddr addr : list) {
                RedisClient redisClient = RedisClientHub.get(addr);
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
                RedisClient redisClient = RedisClientHub.get(addr);
                if (redisClient != null && redisClient.isValid()) {
                    return addr;
                } else {
                    dynamicList.remove(addr);
                }
            }
            int i = ThreadLocalRandom.current().nextInt(list.size());
            return list.get(i);
        } catch (Exception e) {
            if (list.isEmpty()) return null;
            int i = ThreadLocalRandom.current().nextInt(list.size());
            return list.get(i);
        }
    }

    @Override
    public Resource getResource() {
        return redisProxiesResource;
    }
}
