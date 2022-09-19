package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.model.*;
import com.netease.nim.camellia.redis.proxy.plugin.bigkey.BigKeyMonitor;
import com.netease.nim.camellia.redis.proxy.plugin.hotkey.HotKeyMonitor;
import com.netease.nim.camellia.redis.proxy.plugin.monitor.CommandCountMonitor;
import com.netease.nim.camellia.redis.proxy.plugin.monitor.CommandSpendMonitor;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class ProxyMonitorCollector {

    private static final Logger logger = LoggerFactory.getLogger(ProxyMonitorCollector.class);

    private static final AtomicBoolean initOk = new AtomicBoolean(false);
    private static MonitorCallback monitorCallback;
    private static int intervalSeconds;

    private static boolean monitorEnable;
    private static boolean commandSpendTimeMonitorEnable;
    private static boolean upstreamRedisSpendTimeMonitorEnable;

    private static Stats stats = new Stats();

    public static void init(CamelliaServerProperties serverProperties, MonitorCallback monitorCallback) {
        if (initOk.compareAndSet(false, true)) {
            int seconds = serverProperties.getMonitorIntervalSeconds();
            intervalSeconds = seconds;
            ExecutorUtils.scheduleAtFixedRate(ProxyMonitorCollector::collect, seconds, seconds, TimeUnit.SECONDS);
            ProxyMonitorCollector.monitorEnable = serverProperties.isMonitorEnable();
            ProxyMonitorCollector.monitorCallback = monitorCallback;
            ProxyDynamicConf.registerCallback(ProxyMonitorCollector::reloadConf);
            reloadConf();
            logger.info("RedisMonitor init success, intervalSeconds = {}, monitorEnable = {}", intervalSeconds, monitorEnable);
        }
    }

    private static void reloadConf() {
        ProxyMonitorCollector.monitorEnable = ProxyDynamicConf.getBoolean("monitor.enable", ProxyMonitorCollector.monitorEnable);
        ProxyMonitorCollector.commandSpendTimeMonitorEnable = ProxyDynamicConf.getBoolean("command.spend.time.monitor.enable", true);
        ProxyMonitorCollector.upstreamRedisSpendTimeMonitorEnable = ProxyDynamicConf.getBoolean("upstream.redis.spend.time.monitor.enable", true);
    }

    /**
     * monitorEnable
     * @return monitorEnable
     */
    public static boolean isMonitorEnable() {
        return monitorEnable;
    }

    /**
     * 对命令耗时监控专门加一个子开关
     */
    public static boolean isCommandSpendTimeMonitorEnable() {
        return monitorEnable && commandSpendTimeMonitorEnable;
    }

    /**
     * 对上游耗时监控专门加一个子开关
     */
    public static boolean isUpstreamRedisSpendTimeMonitorEnable() {
        return monitorEnable && upstreamRedisSpendTimeMonitorEnable;
    }

    /**
     * get Stats
     */
    public static Stats getStats() {
        return stats;
    }

    /**
     * 定时计算
     */
    private static void collect() {
        try {
            Stats stats = new Stats();
            stats.setIntervalSeconds(intervalSeconds);
            stats.setClientConnectCount(ChannelMonitor.connect());

            CommandCountMonitor.CommandCounterStats commandCounterStats = CommandCountMonitor.collect();
            stats.setCount(commandCounterStats.count);
            stats.setTotalReadCount(commandCounterStats.totalReadCount);
            stats.setTotalWriteCount(commandCounterStats.totalWriteCount);
            stats.setDetailStatsList(commandCounterStats.detailStatsList);
            stats.setTotalStatsList(commandCounterStats.totalStatsList);
            stats.setBidBgroupStatsList(commandCounterStats.bidBgroupStatsList);

            stats.setFailMap(CommandFailMonitor.collect());

            CommandSpendMonitor.CommandSpendStats commandSpendStats = CommandSpendMonitor.collect();
            stats.setSpendStatsList(commandSpendStats.spendStatsList);
            stats.setBidBgroupSpendStatsList(commandSpendStats.bidBgroupSpendStatsList);

            ResourceStatsMonitor.ResourceStatsCollect resourceStats = ResourceStatsMonitor.collect();
            stats.setResourceStatsList(resourceStats.resourceStatsList);
            stats.setResourceCommandStatsList(resourceStats.resourceCommandStatsList);
            stats.setResourceBidBgroupStatsList(resourceStats.resourceBidBgroupStatsList);
            stats.setResourceBidBgroupCommandStatsList(resourceStats.resourceBidBgroupCommandStatsList);

            stats.setRouteConfList(RouteConfMonitor.collect());
            stats.setRedisConnectStats(RedisClientMonitor.collect());
            stats.setUpstreamRedisSpendStatsList(UpstreamRedisSpendTimeMonitor.collect());
            stats.setBigKeyStatsList(BigKeyMonitor.collect());
            stats.setHotKeyStatsList(HotKeyMonitor.collect());
            stats.setSlowCommandStatsList(SlowCommandMonitor.collect());
            stats.setHotKeyCacheStatsList(HotKeyCacheMonitor.collect());

            ProxyMonitorCollector.stats = stats;

            if (monitorCallback != null) {
                monitorCallback.callback(stats);
            }
            ProxyInfoUtils.updateStats(stats);
        } catch (Exception e) {
            logger.error("monitor data collect error", e);
        }
    }

}
