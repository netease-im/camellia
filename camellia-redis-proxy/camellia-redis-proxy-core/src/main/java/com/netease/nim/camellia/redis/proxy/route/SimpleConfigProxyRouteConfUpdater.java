package com.netease.nim.camellia.redis.proxy.route;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.tools.config.SimpleConfigFetcher;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2025/8/14
 */
public class SimpleConfigProxyRouteConfUpdater extends ProxyRouteConfUpdater {

    private static final Logger logger = LoggerFactory.getLogger(SimpleConfigProxyRouteConfUpdater.class);

    private final ConcurrentHashMap<String, SimpleConfigFetcher> fetcherMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> bizBidBgroupMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> configMap = new ConcurrentHashMap<>();

    public SimpleConfigProxyRouteConfUpdater() {
        int intervalSeconds = ProxyDynamicConf.getInt("simple.config.reload.interval.seconds", 1);
        ExecutorUtils.scheduleAtFixedRate(this::reload, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void reload() {
        try {
            for (Map.Entry<String, SimpleConfigFetcher> entry : fetcherMap.entrySet()) {
                try {
                    String biz = entry.getKey();
                    SimpleConfigFetcher fetcher = entry.getValue();
                    String newConfig = fetcher.getConfig();
                    String oldConfig = configMap.get(biz);
                    if (!Objects.equals(newConfig, oldConfig)) {
                        ResourceTable resourceTable = parse(newConfig);
                        configMap.put(biz, newConfig);
                        ConcurrentHashMap<String, Boolean> subMap = bizBidBgroupMap.get(biz);
                        for (String key : subMap.keySet()) {
                            String[] split = key.split("\\|");
                            long bid = Long.parseLong(split[0]);
                            String bgroup = split[1];
                            invokeUpdateResourceTable(bid, bgroup, resourceTable);
                        }
                    }
                } catch (Exception e) {
                    logger.error("simple config reload error");
                }
            }
        } catch (Exception e) {
            logger.error("reload error", e);
        }
    }

    @Override
    public ResourceTable getResourceTable(long bid, String bgroup) {
        String defaultBiz;
        if (bid < 0 || bgroup == null) {
            defaultBiz = "default";
        } else {
            defaultBiz = bid + "_" + bgroup;
        }
        String biz = ProxyDynamicConf.getString("simple.config.biz", bid, bgroup, defaultBiz);

        ConcurrentHashMap<String, Boolean> subMap = CamelliaMapUtils.computeIfAbsent(bizBidBgroupMap, biz, k -> new ConcurrentHashMap<>());
        subMap.put(biz + "|" + bgroup, Boolean.TRUE);

        String url = ProxyDynamicConf.getString("simple.config.fetch.url", null);
        SimpleConfigFetcher fetcher = CamelliaMapUtils.computeIfAbsent(fetcherMap, biz, k -> new SimpleConfigFetcher(url, biz));

        String config = fetcher.getConfig();
        ResourceTable resourceTable = parse(config);

        configMap.put(biz, config);

        return resourceTable;
    }

    private ResourceTable parse(String config) {
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(config);
        checkResourceTable(resourceTable);
        return resourceTable;
    }
}
