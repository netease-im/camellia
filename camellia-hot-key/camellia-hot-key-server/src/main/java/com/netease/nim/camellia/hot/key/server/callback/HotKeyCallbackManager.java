package com.netease.nim.camellia.hot.key.server.callback;

import com.netease.nim.camellia.hot.key.common.model.HotKey;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.common.netty.pack.HotKeyCacheStats;
import com.netease.nim.camellia.hot.key.server.calculate.TopNStatsResult;
import com.netease.nim.camellia.hot.key.server.event.ValueGetter;
import com.netease.nim.camellia.hot.key.server.monitor.HotKeyCollector;
import com.netease.nim.camellia.hot.key.server.monitor.HotKeyServerStats;
import com.netease.nim.camellia.hot.key.server.utils.TimeCache;
import com.netease.nim.camellia.hot.key.server.bean.BeanInitUtils;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.tools.cache.NamespaceCamelliaLocalCache;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/5/10
 */
public class HotKeyCallbackManager {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyCallbackManager.class);

    private final HotKeyServerProperties properties;
    private final ThreadPoolExecutor executor;
    private final HotKeyCallback hotKeyCallback;
    private final HotKeyTopNCallback topNCallback;
    private final HotKeyCacheStatsCallback hotKeyCacheStatsCallback;
    private final MonitorCallback monitorCallback;
    private final NamespaceCamelliaLocalCache callbackTimeCache;

    public HotKeyCallbackManager(HotKeyServerProperties properties) {
        this.properties = properties;
        this.hotKeyCallback = (HotKeyCallback) properties.getBeanFactory().getBean(BeanInitUtils.parseClass(properties.getHotKeyCallbackClassName()));
        this.topNCallback = (HotKeyTopNCallback) properties.getBeanFactory().getBean(BeanInitUtils.parseClass(properties.getTopNCallbackClassName()));
        this.hotKeyCacheStatsCallback = (HotKeyCacheStatsCallback) properties.getBeanFactory().getBean(BeanInitUtils.parseClass(properties.getHotKeyCacheStatsCallbackClassName()));
        this.monitorCallback = (MonitorCallback) properties.getBeanFactory().getBean(BeanInitUtils.parseClass(properties.getMonitorCallbackClassName()));
        this.callbackTimeCache = new NamespaceCamelliaLocalCache(properties.getMaxNamespace(), properties.getHotKeyCacheCounterCapacity());
        this.executor = new ThreadPoolExecutor(properties.getCallbackExecutorSize(), properties.getCallbackExecutorSize(), 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10000), new CamelliaThreadFactory("hot-key-callback"), new ThreadPoolExecutor.DiscardPolicy());
        logger.info("HotKeyCallbackManager init success, HotKeyCallback = {}, HotKeyTopNCallback = {}",
                hotKeyCallback.getClass().getName(), topNCallback.getClass().getName());
    }

    /**
     * 发现一个新的热key的回调
     */
    public void newHotkey(HotKey hotKey, Rule rule, ValueGetter getter) {
        try {
            Long lastCallbackTime = callbackTimeCache.get(hotKey.getNamespace(), hotKey.getKey(), Long.class);
            if (lastCallbackTime == null) {
                boolean success = callbackTimeCache.putIfAbsent(hotKey.getNamespace(), hotKey.getKey(),
                        TimeCache.currentMillis, properties.getHotKeyCallbackIntervalSeconds());
                if (success) {
                    HotKeyInfo hotKeyInfo = new HotKeyInfo(hotKey.getNamespace(), hotKey.getKey(), hotKey.getAction(), rule, getter.get());
                    executor.submit(() -> {
                        try {
                            HotKeyCollector.update(hotKeyInfo);
                            hotKeyCallback.newHotKey(hotKeyInfo);
                        } catch (Exception e) {
                            logger.error("hotKeyInfo callback error", e);
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("submit hotKeyInfo callback error", e);
        }
    }

    /**
     * topN统计数据，注意这是全局的回调，而非单机的回调
     * @param result stats
     */
    public void topN(TopNStatsResult result) {
        try {
            executor.submit(() -> {
                try {
                    topNCallback.topN(result);
                } catch (Exception e) {
                    logger.error("topN callback error", e);
                }
            });
        } catch (Exception e) {
            logger.error("submit topN callback error", e);
        }
    }

    /**
     * 缓存命中情况的统计数据回调
     * @param statsList 统计数据
     */
    public void newStats(List<HotKeyCacheStats> statsList) {
        try {
            executor.submit(() -> {
                try {
                    hotKeyCacheStatsCallback.newStats(statsList);
                } catch (Exception e) {
                    logger.error("newStats callback error", e);
                }
            });
        } catch (Exception e) {
            logger.error("submit newStats callback error");
        }
    }

    /**
     * 统计信息
     * @param serverStats serverStats
     */
    public void serverStats(HotKeyServerStats serverStats) {
        try {
            executor.submit(() -> {
                try {
                    monitorCallback.serverStats(serverStats);
                } catch (Exception e) {
                    logger.error("serverStats callback error", e);
                }
            });
        } catch (Exception e) {
            logger.error("submit serverStats callback error");
        }
    }
}
