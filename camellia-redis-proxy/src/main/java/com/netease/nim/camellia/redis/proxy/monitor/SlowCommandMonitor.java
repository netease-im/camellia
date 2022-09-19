package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.model.SlowCommandStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * Created by caojiajun on 2020/12/1
 */
public class SlowCommandMonitor {

    private static final Logger logger = LoggerFactory.getLogger(SlowCommandMonitor.class);

    private static final int defaultMaxCount = 100;
    private static final LinkedBlockingQueue<SlowCommandStats> queue;
    static {
        queue = new LinkedBlockingQueue<>(ProxyDynamicConf.getInt("slow.command.monitor.json.max.count", defaultMaxCount) * 10);
    }

    public static void slowCommand(Command command, double spendMillis, long thresholdMillis) {
        try {
            if (spendMillis <= thresholdMillis) return;
            if (queue.size() >= ProxyDynamicConf.getInt("slow.command.monitor.json.max.count", defaultMaxCount)) {
                return;
            }
            Long bid = command.getCommandContext().getBid();
            String bgroup = command.getCommandContext().getBgroup();
            SlowCommandStats slowCommandStats = new SlowCommandStats();
            slowCommandStats.setBid(bid == null ? "default" : String.valueOf(bid));
            slowCommandStats.setBgroup(bgroup == null ? "default" : bgroup);
            slowCommandStats.setCommand(command.getName());
            slowCommandStats.setKeys(command.getKeysStr());
            slowCommandStats.setSpendMillis(spendMillis);
            slowCommandStats.setThresholdMillis(thresholdMillis);
            queue.offer(slowCommandStats);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static List<SlowCommandStats> collect() {
        List<SlowCommandStats> list = new ArrayList<>();
        while (!queue.isEmpty()) {
            SlowCommandStats slowCommandStats = queue.poll();
            if (slowCommandStats == null) break;
            list.add(slowCommandStats);
        }
        return list;
    }

}
