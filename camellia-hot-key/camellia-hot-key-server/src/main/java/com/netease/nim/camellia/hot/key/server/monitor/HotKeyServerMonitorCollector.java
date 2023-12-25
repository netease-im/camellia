package com.netease.nim.camellia.hot.key.server.monitor;

import com.netease.nim.camellia.hot.key.server.callback.HotKeyCallbackManager;
import com.netease.nim.camellia.hot.key.server.callback.HotKeyInfo;
import com.netease.nim.camellia.hot.key.server.conf.ClientConnectHub;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.sys.CpuUsageCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2023/5/11
 */
public class HotKeyServerMonitorCollector {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyServerMonitorCollector.class);

    private static final AtomicBoolean initOk = new AtomicBoolean(false);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("hot-key-server-monitor"));
    private static int monitorIntervalSeconds = 60;
    private static String applicationName;
    private static HotKeyCallbackManager callbackManager;
    private static CpuUsageCollector cpuUsageCollector;

    public static void init(HotKeyServerProperties properties, HotKeyCallbackManager callbackManager) {
        if (initOk.compareAndSet(false, true)) {
            HotKeyCollector.init(properties);
            monitorIntervalSeconds = properties.getMonitorIntervalSeconds();
            applicationName = properties.getApplicationName();
            scheduler.scheduleAtFixedRate(HotKeyServerMonitorCollector::collect, properties.getMonitorIntervalSeconds(), properties.getMonitorIntervalSeconds(), TimeUnit.SECONDS);
            logger.info("HotKeyServerMonitorCollector init success, monitor-interval-seconds = {}", properties.getMonitorIntervalSeconds());
            HotKeyServerMonitorCollector.callbackManager = callbackManager;
            cpuUsageCollector = new CpuUsageCollector(monitorIntervalSeconds);
        } else {
            logger.warn("duplicate HotKeyServerMonitorCollector init");
            cpuUsageCollector = new CpuUsageCollector();
        }
    }

    public static HotKeyServerStats serverStats = new HotKeyServerStats();

    public static HotKeyServerStats getHotKeyServerStats() {
        return serverStats;
    }

    public static CpuUsageCollector getCpuUsageCollector() {
        return cpuUsageCollector;
    }

    private static void collect() {
        HotKeyServerStats serverStats = new HotKeyServerStats();
        serverStats.setApplicationName(applicationName);
        serverStats.setMonitorIntervalSeconds(monitorIntervalSeconds);
        serverStats.setConnectCount(ClientConnectHub.getInstance().getMap().size());
        QueueStats queueStats = HotKeyCalculatorQueueMonitor.collect();
        TrafficStats trafficStats = HotKeyCalculatorMonitorCollector.collect();
        List<HotKeyInfo> hotKeyInfoList = HotKeyCollector.collect();

        serverStats.setQueueStats(queueStats);
        serverStats.setTrafficStats(trafficStats);
        serverStats.setHotKeyInfoList(hotKeyInfoList);

        HotKeyServerMonitorCollector.serverStats = serverStats;
        callbackManager.serverStats(serverStats);
    }
}
