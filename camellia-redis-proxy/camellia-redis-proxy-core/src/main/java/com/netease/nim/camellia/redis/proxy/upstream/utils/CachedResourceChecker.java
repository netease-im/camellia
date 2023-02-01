package com.netease.nim.camellia.redis.proxy.upstream.utils;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.ResourceChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2023/2/1
 */
public class CachedResourceChecker implements ResourceChooser.ResourceChecker {

    private static final Logger logger = LoggerFactory.getLogger(CachedResourceChecker.class);

    private final ConcurrentHashMap<String, Boolean> validCache = new ConcurrentHashMap<>();

    private static final CachedResourceChecker instance = new CachedResourceChecker();
    private CachedResourceChecker() {
    }

    public static CachedResourceChecker getInstance() {
        return instance;
    }

    public void updateCache(Resource resource, boolean valid) {
        if (!valid) {
            logger.warn("resource = {} not valid", resource.getUrl());
        }
        validCache.put(resource.getUrl(), valid);
    }

    @Override
    public boolean checkValid(Resource resource) {
        Boolean valid = validCache.get(resource.getUrl());
        return valid == null || valid;
    }
}
