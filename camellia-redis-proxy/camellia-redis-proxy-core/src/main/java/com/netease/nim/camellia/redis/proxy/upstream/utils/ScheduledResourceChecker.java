package com.netease.nim.camellia.redis.proxy.upstream.utils;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.ResourceSelector;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.redis.proxy.upstream.UpstreamRedisClientFactory;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/2/1
 */
public class ScheduledResourceChecker implements ResourceSelector.ResourceChecker {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledResourceChecker.class);

    private final ConcurrentHashMap<String, IUpstreamClient> clientCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> validCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastCheckValidTime = new ConcurrentHashMap<>();

    private final UpstreamRedisClientFactory factory;

    public ScheduledResourceChecker(UpstreamRedisClientFactory factory) {
        this.factory = factory;
        int intervalSeconds = ProxyDynamicConf.getInt("check.redis.resource.valid.interval.seconds", 5);
        ExecutorUtils.scheduleAtFixedRate(this::schedule, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        logger.info("ScheduledResourceChecker start, intervalSeconds = {}", intervalSeconds);
    }

    private void schedule() {
        try {
            long notActiveThresholdMs = ProxyDynamicConf.getInt("check.redis.resource.valid.not.active.threshold.seconds", 300) * 1000L;
            Set<String> notActiveClients = new HashSet<>();
            for (Map.Entry<String, IUpstreamClient> entry : clientCache.entrySet()) {
                String url = entry.getKey();
                boolean valid = entry.getValue().isValid();
                if (!valid) {
                    logger.warn("IUpstreamClient with resource = {} not valid", url);
                }
                validCache.put(url, valid);
                Long lastCheckTime = lastCheckValidTime.get(url);
                if (lastCheckTime != null && TimeCache.currentMillis - lastCheckTime > notActiveThresholdMs) {
                    notActiveClients.add(url);
                }
            }
            if (!notActiveClients.isEmpty()) {
                for (String url : notActiveClients) {
                    clientCache.remove(url);
                    validCache.remove(url);
                    lastCheckValidTime.remove(url);
                }
            }
        } catch (Exception e) {
            logger.error("ScheduledResourceChecker error", e);
        }
    }

    @Override
    public void addResource(Resource resource) {
        String url = resource.getUrl();
        IUpstreamClient client = factory.get(url);
        clientCache.put(url, client);
        boolean valid = client.isValid();
        logger.info("addResource to ScheduledResourceChecker, resource = {}, valid = {}", url, valid);
        validCache.put(url, valid);
        lastCheckValidTime.put(url, TimeCache.currentMillis);
    }

    @Override
    public boolean checkValid(Resource resource) {
        String url = resource.getUrl();
        Boolean valid = validCache.get(url);
        lastCheckValidTime.put(url, TimeCache.currentMillis);
        return valid == null || valid;
    }
}
