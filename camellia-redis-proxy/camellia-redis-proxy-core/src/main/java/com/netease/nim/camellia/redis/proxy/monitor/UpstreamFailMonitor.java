package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.model.UpstreamFailStats;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2023/3/13
 */
public class UpstreamFailMonitor {

    private static final Logger logger = LoggerFactory.getLogger(CommandFailMonitor.class);
    private static final Logger statsLogger = LoggerFactory.getLogger("camellia-failed-command-stats");

    private static ConcurrentHashMap<String, LongAdder> failCountMap = new ConcurrentHashMap<>();

    private static final AtomicBoolean backUp = new AtomicBoolean(false);
    private static final LinkedBlockingQueue<FailedCommand> failedCommands1;
    private static final LinkedBlockingQueue<FailedCommand> failedCommands2;
    static {
        int size = ProxyDynamicConf.getInt("failed.command.max.cache.capacity", 1000);
        failedCommands1 = new LinkedBlockingQueue<>(size);
        failedCommands2 = new LinkedBlockingQueue<>(size);
        int intervalSeconds = ProxyDynamicConf.getInt("failed.command.schedule.interval.seconds", 5);
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-failed-command"))
                .scheduleAtFixedRate(UpstreamFailMonitor::schedule, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private static void schedule() {
        try {
            LinkedBlockingQueue<FailedCommand> queue = null;
            if (backUp.get()) {
                if (backUp.compareAndSet(true, false)) {
                    queue = failedCommands2;
                }
            } else {
                if (backUp.compareAndSet(false, true)) {
                    queue = failedCommands1;
                }
            }
            if (queue == null) return;
            if (queue.isEmpty()) return;
            while (true) {
                FailedCommand failedCommand = queue.poll();
                if (failedCommand == null) {
                    break;
                }
                ChannelInfo channelInfo = failedCommand.command.getChannelInfo();
                SocketAddress clientAddr = channelInfo.getClientSocketAddress();
                statsLogger.error("command failed, resource = {}, command = {}, keys = {}, error = {}, client.addr = {}",
                        PasswordMaskUtils.maskResource(failedCommand.resource), failedCommand.command.getName(),
                        failedCommand.command.getKeysStr(), failedCommand.error, clientAddr);
            }
        } catch (Exception e) {
            logger.error("failed command schedule error", e);
        }
    }

    /**
     * 增加上游转发失败的监控埋点
     * @param resource resource
     * @param command command
     * @param future future
     */
    public static void stats(String resource, Command command, CompletableFuture<Reply> future) {
        future.thenAccept(reply -> stats(resource, command, reply));
    }

    /**
     * 增加上游转发失败的监控埋点
     * @param resource resource
     * @param command command
     * @param reply reply
     */
    public static void stats(String resource, String command, Reply reply) {
        if (reply instanceof ErrorReply) {
            try {
                String key = resource + "|" + command + "|" + ((ErrorReply) reply).getError();
                LongAdder failCount = CamelliaMapUtils.computeIfAbsent(failCountMap, key, k -> new LongAdder());
                failCount.increment();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 增加上游转发失败的监控埋点
     * @param resource resource
     * @param command command
     * @param reply reply
     */
    public static void stats(String resource, Command command, Reply reply) {
        if (command != null) {
            stats(resource, command.getName(), reply);
            if (reply instanceof ErrorReply) {
                if (backUp.get()) {
                    failedCommands2.offer(new FailedCommand(resource, command, ((ErrorReply) reply).getError()));
                } else {
                    failedCommands1.offer(new FailedCommand(resource, command, ((ErrorReply) reply).getError()));
                }
            }
        }
    }

    private static class FailedCommand {
        String resource;
        Command command;
        String error;

        public FailedCommand(String resource, Command command, String error) {
            this.resource = resource;
            this.command = command;
            this.error = error;
        }
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
