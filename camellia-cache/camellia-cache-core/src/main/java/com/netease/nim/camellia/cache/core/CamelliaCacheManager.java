package com.netease.nim.camellia.cache.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;

import java.util.*;

public class CamelliaCacheManager extends AbstractCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaCacheManager.class);

    private final List<CamelliaCache> list = new ArrayList<>();
    private final Set<String> cacheNameSet = new HashSet<>();

    public <T extends INativeCache> void addCamelliaCache(CamelliaCacheConfig<T> cacheConfig) {
        if (cacheConfig == null) return;
        addCamelliaCacheList(Collections.singletonList(cacheConfig));
    }

    public <T extends INativeCache> void addCamelliaCacheList(List<CamelliaCacheConfig<T>> cacheConfigList) {
        if (cacheConfigList == null || cacheConfigList.isEmpty()) return;
        for (CamelliaCacheConfig<T> config : cacheConfigList) {
            if (config == null) continue;
            if (cacheNameSet.contains(config.getName())) {
                logger.warn("cacheName = {} duplicate, will cover the old one.", config.getName());
            }
            cacheNameSet.add(config.getName());
            list.add(new CamelliaCache(config));
        }
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        return list;
    }
}
