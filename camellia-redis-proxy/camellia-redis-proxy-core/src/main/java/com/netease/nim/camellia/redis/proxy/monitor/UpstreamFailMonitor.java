package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.UpstreamFailStats;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2023/3/13
 */
public class UpstreamFailMonitor {

    private static final Logger logger = LoggerFactory.getLogger(CommandFailMonitor.class);

    private static ConcurrentHashMap<String, LongAdder> failCountMap = new ConcurrentHashMap<>();

    /**
     * 增加上游转发失败的监控埋点
     * @param resource resource
     * @param future future
     */
    public static void stats(String resource, String command, CompletableFuture<Reply> future) {
        future.thenAccept(reply -> {
            if (reply instanceof ErrorReply) {
                try {
                    String key = resource + "|" + command + "|" + ((ErrorReply) reply).getError();
                    LongAdder failCount = CamelliaMapUtils.computeIfAbsent(failCountMap, key, k -> new LongAdder());
                    failCount.increment();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
    }

    public static List<UpstreamFailStats> collect() {
        ConcurrentHashMap<String, LongAdder> failCountMap = UpstreamFailMonitor.failCountMap;
        UpstreamFailMonitor.failCountMap = new ConcurrentHashMap<>();
        List<UpstreamFailStats> list = new ArrayList<>();
        for (Map.Entry<String, LongAdder> entry : failCountMap.entrySet()) {
            String[] split = entry.getKey().split("\\|");
            String resource = split[0];
            String command = split[1];
            String msg = split[2];
            UpstreamFailStats stats = new UpstreamFailStats();
            stats.setResource(PasswordMaskUtils.maskResource(resource));
            stats.setCommand(command);
            stats.setMsg(msg);
            stats.setCount(entry.getValue().sum());
            list.add(stats);
        }
        return list;
    }
}
