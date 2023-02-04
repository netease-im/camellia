package com.netease.nim.camellia.redis.proxy.upstream.utils;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.ResourceChooser;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.redis.proxy.upstream.UpstreamRedisClientFactory;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/2/1
 */
public class ScheduledResourceChecker implements ResourceChooser.ResourceChecker {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledResourceChecker.class);

    private final ConcurrentHashMap<String, IUpstreamClient> clientCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> validCache = new ConcurrentHashMap<>();

    private final UpstreamRedisClientFactory factory;

    public ScheduledResourceChecker(UpstreamRedisClientFactory factory) {
        this.factory = factory;
        int intervalSeconds = ProxyDynamicConf.getInt("check.redis.resource.valid.interval.seconds", 5);
        ExecutorUtils.scheduleAtFixedRate(this::schedule, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void schedule() {
        for (Map.Entry<String, IUpstreamClient> entry : clientCache.entrySet()) {
            String url = entry.getKey();
            boolean valid = entry.getValue().isValid();
            if (!valid) {
                logger.warn("IUpstreamClient with resource = {} not valid", url);
            }
            validCache.put(url, valid);
        }
    }

    @Override
    public void addResource(Resource resource) {
        String url = resource.getUrl();
        IUpstreamClient client = factory.get(url);
        clientCache.put(url, client);
        boolean valid = client.isValid();
        if (!valid) {
            logger.warn("IUpstreamClient with resource = {} not valid", url);
        }
        validCache.put(url, valid);
    }

    @Override
    public boolean checkValid(Resource resource) {
        Boolean valid = validCache.get(resource.getUrl());
        return valid == null || valid;
    }
}
