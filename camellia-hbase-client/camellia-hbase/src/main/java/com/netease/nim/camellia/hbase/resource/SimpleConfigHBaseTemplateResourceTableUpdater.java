package com.netease.nim.camellia.hbase.resource;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.hbase.util.HBaseResourceUtil;
import com.netease.nim.camellia.tools.config.SimpleConfigFetcher;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2025/8/6
 */
public class SimpleConfigHBaseTemplateResourceTableUpdater extends HBaseTemplateResourceTableUpdater {

    private static final Logger logger = LoggerFactory.getLogger(SimpleConfigHBaseTemplateResourceTableUpdater.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(),
            new CamelliaThreadFactory(SimpleConfigHBaseTemplateResourceTableUpdater.class));

    private final String biz;
    private final SimpleConfigFetcher fetcher;
    private ResourceTable resourceTable;
    private String config;

    public SimpleConfigHBaseTemplateResourceTableUpdater() {
        this(System.getProperty("simple.config.fetch.url"), System.getProperty("simple.config.hbase.biz"));
    }

    public SimpleConfigHBaseTemplateResourceTableUpdater(String biz, String url) {
        this.biz = biz;
        this.fetcher = new SimpleConfigFetcher(biz, url);
        fetch();
        if (resourceTable == null) {
            throw new IllegalStateException("init hbase resource table error, biz = " + biz + ", url = " + url);
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
            if (Objects.equals(newConfig, config)) {
                return;
            }
            ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(newConfig);
            HBaseResourceUtil.checkResourceTable(resourceTable);
            this.resourceTable = resourceTable;
            this.config = newConfig;
            invokeUpdateResourceTable(resourceTable);
            logger.info("hbase config updated, biz = {}, config = {}", biz, newConfig);
        } catch (Exception e) {
            logger.error("hbase redis config error, biz = {}", biz, e);
        }
    }
}
