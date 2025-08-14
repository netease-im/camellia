package com.netease.nim.camellia.redis.resource;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.tools.config.SimpleConfigFetcher;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2025/8/14
 */
public class SimpleConfigRedisTemplateResourceTableUpdater extends RedisTemplateResourceTableUpdater {

    private static final Logger logger = LoggerFactory.getLogger(SimpleConfigRedisTemplateResourceTableUpdater.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(),
            new CamelliaThreadFactory(SimpleConfigRedisTemplateResourceTableUpdater.class));

    private final String biz;
    private final SimpleConfigFetcher fetcher;
    private ResourceTable resourceTable;
    private String config;

    public SimpleConfigRedisTemplateResourceTableUpdater() {
        this(System.getProperty("simple.config.fetch.url"), System.getProperty("simple.config.redis.biz"));
    }

    public SimpleConfigRedisTemplateResourceTableUpdater(String biz, String url) {
        this.biz = biz;
        this.fetcher = new SimpleConfigFetcher(biz, url);
        fetch();
        if (resourceTable == null) {
            throw new IllegalStateException("init redis resource table error, biz = " + biz + ", url = " + url);
        }
        scheduler.scheduleAtFixedRate(this::fetch, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public ResourceTable getResourceTable() {
        return resourceTable;
    }

    private void fetch() {
        try {
            String newConfig = fetcher.getConfig();
            if (Objects.equals(config, newConfig)) {
                return;
            }
            ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(newConfig);
            RedisClientResourceUtil.checkResourceTable(resourceTable);
            this.resourceTable = resourceTable;
            this.config = newConfig;
            invokeUpdateResourceTable(resourceTable);
            logger.info("redis config updated, biz = {}, config = {}", biz, newConfig);
        } catch (Exception e) {
            logger.error("fetch redis config error, biz = {}", biz, e);
        }
    }
}
