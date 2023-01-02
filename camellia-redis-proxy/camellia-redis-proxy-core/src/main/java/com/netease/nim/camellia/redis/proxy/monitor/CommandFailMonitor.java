package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2022/9/16
 */
public class CommandFailMonitor {

    private static final Logger logger = LoggerFactory.getLogger(CommandFailMonitor.class);

    private static ConcurrentHashMap<String, LongAdder> failCountMap = new ConcurrentHashMap<>();

    /**
     * command fail incr
     */
    public static void incr(String failReason) {
        try {
            LongAdder failCount = CamelliaMapUtils.computeIfAbsent(failCountMap, failReason, k -> new LongAdder());
            failCount.increment();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static Map<String, Long> collect() {
        ConcurrentHashMap<String, LongAdder> failCountMap = CommandFailMonitor.failCountMap;
        CommandFailMonitor.failCountMap = new ConcurrentHashMap<>();
        Map<String, Long> map = new HashMap<>();
        for (Map.Entry<String, LongAdder> entry : failCountMap.entrySet()) {
            map.put(entry.getKey(), entry.getValue().sum());
        }
        return map;
    }
}
