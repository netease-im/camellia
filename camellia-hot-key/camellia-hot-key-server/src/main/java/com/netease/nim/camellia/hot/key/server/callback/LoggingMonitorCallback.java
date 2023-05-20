package com.netease.nim.camellia.hot.key.server.callback;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.netease.nim.camellia.hot.key.server.monitor.HotKeyServerStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2023/5/17
 */
public class LoggingMonitorCallback implements MonitorCallback {

    private static final Logger statsLogger = LoggerFactory.getLogger("camellia-monitor-collect");

    @Override
    public void serverStats(HotKeyServerStats serverStats) {
        statsLogger.info("serverStats = \n{}\n",
                JSON.toJSONString(serverStats, SerializerFeature.PrettyFormat));
    }
}
