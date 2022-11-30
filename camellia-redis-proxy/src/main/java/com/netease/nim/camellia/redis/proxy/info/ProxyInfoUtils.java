package com.netease.nim.camellia.redis.proxy.info;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.monitor.model.Stats;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.upstream.AsyncCamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.AsyncCamelliaRedisTemplateChooser;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClient;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientAddr;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.*;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.*;
import java.nio.charset.StandardCharsets;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2021/6/24
 */
public class ProxyInfoUtils {

    private static final Logger logger = LoggerFactory.getLogger(ProxyInfoUtils.class);

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(8), new DefaultThreadFactory("proxy-info"));

    public static final String VERSION = "v1.1.7";
    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private static final List<GarbageCollectorMXBean> garbageCollectorMXBeanList = ManagementFactory.getGarbageCollectorMXBeans();

    private static int monitorIntervalSeconds;
    private static final AtomicLong commandsCount = new AtomicLong();
    private static final AtomicLong readCommandsCount = new AtomicLong();
    private static final AtomicLong writeCommandsCount = new AtomicLong();

    private static double avgCommandsQps = 0.0;
    private static double avgReadCommandsQps = 0.0;
    private static double avgWriteCommandsQps = 0.0;

    private static double lastCommandQps = 0.0;
    private static double lastReadCommandQps = 0.0;
    private static double lastWriteCommandQps = 0.0;

    public static void updateStats(Stats stats) {
        try {
            monitorIntervalSeconds = stats.getIntervalSeconds();
            commandsCount.addAndGet(stats.getCount());
            readCommandsCount.addAndGet(stats.getTotalReadCount());
            writeCommandsCount.addAndGet(stats.getTotalWriteCount());

            avgCommandsQps = commandsCount.get() / (runtimeMXBean.getUptime() / 1000.0);
            avgReadCommandsQps = readCommandsCount.get() / (runtimeMXBean.getUptime() / 1000.0);
            avgWriteCommandsQps = writeCommandsCount.get() / (runtimeMXBean.getUptime() / 1000.0);

            lastCommandQps = (double) stats.getCount() / stats.getIntervalSeconds();
            lastReadCommandQps = (double) stats.getTotalReadCount() / stats.getIntervalSeconds();
            lastWriteCommandQps = (double) stats.getTotalWriteCount() / stats.getIntervalSeconds();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static CompletableFuture<Reply> getInfoReply(Command command, AsyncCamelliaRedisTemplateChooser chooser) {
        CompletableFuture<Reply> future = new CompletableFuture<>();
        try {
            executor.submit(() -> {
                Reply reply = generateInfoReply(command, chooser);
                future.complete(reply);
            });
            return future;
        } catch (Exception e) {
            ErrorLogCollector.collect(ProxyInfoUtils.class, "submit generateInfoReply task error", e);
            future.complete(ErrorReply.TOO_BUSY);
            return future;
        }
    }

    public static String generateProxyInfo(Map<String, String> params) {
        String json = params.get("json");
        boolean parseJson = json != null && json.equalsIgnoreCase("true");
        String section = params.get("section");
        if (section != null) {
            if (section.equalsIgnoreCase("upstream-info")) {
                String bid = params.get("bid");
                String bgroup = params.get("bgroup");
                if (bid == null || bgroup == null) {
                    return UpstreamInfoUtils.upstreamInfo(null, null, GlobalRedisProxyEnv.chooser, parseJson);
                }
                try {
                    Long.parseLong(bid);
                } catch (NumberFormatException e) {
                    return parseResponse(ErrorReply.SYNTAX_ERROR, parseJson);
                }
                return UpstreamInfoUtils.upstreamInfo(Long.parseLong(bid), bgroup, GlobalRedisProxyEnv.chooser, parseJson);
            } else {
                Reply reply = generateInfoReply(new Command(new byte[][]{RedisCommand.INFO.raw(), section.getBytes(StandardCharsets.UTF_8)}), GlobalRedisProxyEnv.chooser);
                return parseResponse(reply, parseJson);
            }
        } else {
            Reply reply = generateInfoReply(new Command(new byte[][]{RedisCommand.INFO.raw()}), GlobalRedisProxyEnv.chooser);
            return parseResponse(reply, parseJson);
        }
    }

    private static String parseResponse(Reply reply, boolean parseJson) {
        String string = replyToString(reply);
        if (!parseJson) return string;
        String[] split = string.split("\r\n");
        JSONObject result = new JSONObject();
        if (split.length == 1) {
            result.put("code", 500);
            result.put("msg", split[0]);
            return result.toJSONString();
        }
        result.put("code", 200);
        result.put("msg", "success");
        JSONObject data = new JSONObject();
        String key = null;
        JSONObject item = new JSONObject();
        for (String line : split) {
            line = line.replaceAll("\\s*", "");
            if (line.startsWith("#")) {
                line = line.replaceAll("#", "");
                if (key != null) {
                    data.put(key, item);
                    item = new JSONObject();
                }
                key = line;
            } else {
                int index;
                if (line.startsWith("upstream_redis_nums")) {
                    index = line.lastIndexOf(":");
                } else {
                    index = line.indexOf(":");
                }
                if (index > 0) {
                    String itemKey = line.substring(0, index);
                    String itemValue = line.substring(index + 1);
                    if (key != null && key.equalsIgnoreCase("Route")) {
                        try {
                            JSONObject itemValueJson = JSONObject.parseObject(itemValue);
                            item.put(itemKey, itemValueJson);
                        } catch (Exception e) {
                            item.put(itemKey, itemValue);
                        }
                    } else {
                        item.put(itemKey, itemValue);
                    }
                }
            }
        }
        if (key != null) {
            data.put(key, item);
        }
        result.put("data", data);
        return result.toJSONString();
    }

    private static String replyToString(Reply reply) {
        if (reply == null) {
            reply = ErrorReply.SYNTAX_ERROR;
        }
        if (reply instanceof BulkReply) {
            return reply.toString();
        } else if (reply instanceof ErrorReply) {
            return ((ErrorReply) reply).getError();
        } else {
            return ErrorReply.SYNTAX_ERROR.getError();
        }
    }

    public static Reply generateInfoReply(Command command, AsyncCamelliaRedisTemplateChooser chooser) {
        try {
            StringBuilder builder = new StringBuilder();
            byte[][] objects = command.getObjects();
            if (objects.length == 1) {
                builder.append(getServer()).append("\r\n");
                builder.append(getClients()).append("\r\n");
                builder.append(getRoutes()).append("\r\n");
                builder.append(getUpstream()).append("\r\n");
                builder.append(getMemory()).append("\r\n");
                builder.append(getGC()).append("\r\n");
                builder.append(getStats()).append("\r\n");
            } else {
                if (objects.length == 2) {
                    String section = Utils.bytesToString(objects[1]);
                    if (section.equalsIgnoreCase("server")) {
                        builder.append(getServer()).append("\r\n");
                    } else if (section.equalsIgnoreCase("clients")) {
                        builder.append(getClients()).append("\r\n");
                    } else if (section.equalsIgnoreCase("route")) {
                        builder.append(getRoutes()).append("\r\n");
                    } else if (section.equalsIgnoreCase("upstream")) {
                        builder.append(getUpstream()).append("\r\n");
                    } else if (section.equalsIgnoreCase("memory")) {
                        builder.append(getMemory()).append("\r\n");
                    } else if (section.equalsIgnoreCase("gc")) {
                        builder.append(getGC()).append("\r\n");
                    } else if (section.equalsIgnoreCase("stats")) {
                        builder.append(getStats()).append("\r\n");
                    } else if (section.equalsIgnoreCase("upstream-info")) {
                        builder.append(UpstreamInfoUtils.upstreamInfo(null, null, chooser, false)).append("\r\n");
                    }
                } else if (objects.length == 4) {
                    String section = Utils.bytesToString(objects[1]);
                    if (section.equalsIgnoreCase("upstream-info")) {
                        long bid;
                        String bgroup;
                        try {
                            bid = Utils.bytesToNum(objects[2]);
                            bgroup = Utils.bytesToString(objects[3]);
                        } catch (Exception e) {
                            return ErrorReply.SYNTAX_ERROR;
                        }
                        builder.append(UpstreamInfoUtils.upstreamInfo(bid, bgroup, chooser, false)).append("\r\n");
                    } else {
                        return ErrorReply.SYNTAX_ERROR;
                    }
                } else {
                    return ErrorReply.SYNTAX_ERROR;
                }
            }
            return new BulkReply(Utils.stringToBytes(builder.toString()));
        } catch (Exception e) {
            ErrorLogCollector.collect(ProxyInfoUtils.class, "getInfoReply error", e);
            return new ErrorReply("generate proxy info error");
        }
    }

    private static String getServer() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Server").append("\r\n");
        builder.append("camellia_redis_proxy_version:" + VERSION).append("\r\n");
        builder.append("redis_version:6.2.6").append("\r\n");//spring actuator默认会使用info命令返回的redis_version字段来做健康检查，这里直接返回一个固定的版本号
        builder.append("available_processors:").append(osBean.getAvailableProcessors()).append("\r\n");
        builder.append("netty_boss_thread:").append(GlobalRedisProxyEnv.bossThread).append("\r\n");
        builder.append("netty_work_thread:").append(GlobalRedisProxyEnv.workThread).append("\r\n");
        builder.append("arch:").append(osBean.getArch()).append("\r\n");
        builder.append("os_name:").append(osBean.getName()).append("\r\n");
        builder.append("os_version:").append(osBean.getVersion()).append("\r\n");
        builder.append("system_load_average:").append(osBean.getSystemLoadAverage()).append("\r\n");
        builder.append("tcp_port:").append(GlobalRedisProxyEnv.port).append("\r\n");
        builder.append("http_console_port:").append(GlobalRedisProxyEnv.consolePort).append("\r\n");
        long uptime = runtimeMXBean.getUptime();
        long uptimeInSeconds = uptime / 1000L;
        long uptimeInDays = uptime / (1000L * 60 * 60 * 24);
        builder.append("uptime_in_seconds:").append(uptimeInSeconds).append("\r\n");
        builder.append("uptime_in_days:").append(uptimeInDays).append("\r\n");
        builder.append("vm_vendor:").append(runtimeMXBean.getVmVendor()).append("\r\n");
        builder.append("vm_name:").append(runtimeMXBean.getVmName()).append("\r\n");
        builder.append("vm_version:").append(runtimeMXBean.getVmVersion()).append("\r\n");
        builder.append("jvm_info:").append(System.getProperties().get("java.vm.info")).append("\r\n");
        builder.append("java_version:").append(System.getProperties().get("java.version")).append("\r\n");
        return builder.toString();
    }

    private static String getClients() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Clients").append("\r\n");
        builder.append("connect_clients:").append(ChannelMonitor.connect()).append("\r\n");
        ConcurrentHashMap<String, AtomicLong> map = new ConcurrentHashMap<>();
        for (Map.Entry<String, ChannelInfo> entry : ChannelMonitor.getChannelMap().entrySet()) {
            Long bid = entry.getValue().getBid();
            String bgroup = entry.getValue().getBgroup();
            String key;
            if (bid == null || bgroup == null) {
                key = "connect_clients_default_default";
            } else {
                key = "connect_clients_" + bid + "_" + bgroup;
            }
            AtomicLong count = CamelliaMapUtils.computeIfAbsent(map, key, k -> new AtomicLong());
            count.incrementAndGet();
        }
        for (Map.Entry<String, AtomicLong> entry : map.entrySet()) {
            builder.append(entry.getKey()).append(":").append(entry.getValue().get()).append("\r\n");
        }
        return builder.toString();
    }

    private static String getStats() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Stats").append("\r\n");
        builder.append("commands_count:").append(commandsCount).append("\r\n");
        builder.append("read_commands_count:").append(readCommandsCount).append("\r\n");
        builder.append("write_commands_count:").append(writeCommandsCount).append("\r\n");
        builder.append("avg_commands_qps:").append(String.format("%.2f", avgCommandsQps)).append("\r\n");
        builder.append("avg_read_commands_qps:").append(String.format("%.2f", avgReadCommandsQps)).append("\r\n");
        builder.append("avg_write_commands_qps:").append(String.format("%.2f", avgWriteCommandsQps)).append("\r\n");
        builder.append("monitor_interval_seconds:").append(monitorIntervalSeconds).append("\r\n");
        builder.append("last_commands_qps:").append(String.format("%.2f", lastCommandQps)).append("\r\n");
        builder.append("last_read_commands_qps:").append(String.format("%.2f", lastReadCommandQps)).append("\r\n");
        builder.append("last_write_commands_qps:").append(String.format("%.2f", lastWriteCommandQps)).append("\r\n");
        return builder.toString();
    }

    private static String getRoutes() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Route").append("\r\n");
        ConcurrentHashMap<String, AsyncCamelliaRedisTemplate> templateMap = RouteConfMonitor.getTemplateMap();
        builder.append("route_nums:").append(templateMap.size()).append("\r\n");
        for (Map.Entry<String, AsyncCamelliaRedisTemplate> entry : templateMap.entrySet()) {
            String key = entry.getKey();
            String[] split = key.split("\\|");
            Long bid = null;
            if (!split[0].equals("null")) {
                bid = Long.parseLong(split[0]);
            }
            String bgroup = null;
            if (!split[1].equals("null")) {
                bgroup = split[1];
            }
            AsyncCamelliaRedisTemplate template = entry.getValue();
            String routeConf = ReadableResourceTableUtil.readableResourceTable(PasswordMaskUtils.maskResourceTable(template.getResourceTable()));
            builder.append("route_conf_").append(bid == null ? "default" : bid).append("_").append(bgroup == null ? "default" : bgroup).append(":").append(routeConf).append("\r\n");
        }
        return builder.toString();
    }

    private static String getUpstream() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Upstream").append("\r\n");
        ConcurrentHashMap<RedisClientAddr, ConcurrentHashMap<String, RedisClient>> redisClientMap = RedisClientMonitor.getRedisClientMap();
        int upstreamRedisNums = 0;
        for (Map.Entry<RedisClientAddr, ConcurrentHashMap<String, RedisClient>> entry : redisClientMap.entrySet()) {
            upstreamRedisNums += entry.getValue().size();
        }
        builder.append("upstream_redis_nums:").append(upstreamRedisNums).append("\r\n");
        for (Map.Entry<RedisClientAddr, ConcurrentHashMap<String, RedisClient>> entry : redisClientMap.entrySet()) {
            builder.append("upstream_redis_nums").append("[").append(PasswordMaskUtils.maskAddr(entry.getKey().getUrl())).append("]").append(":").append(entry.getValue().size()).append("\r\n");
        }
        return builder.toString();
    }

    private static String getMemory() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Memory").append("\r\n");
        long freeMemory = Runtime.getRuntime().freeMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        builder.append("free_memory:").append(freeMemory).append("\r\n");
        builder.append("free_memory_human:").append(humanReadableByteCountBin(freeMemory)).append("\r\n");
        builder.append("total_memory:").append(totalMemory).append("\r\n");
        builder.append("total_memory_human:").append(humanReadableByteCountBin(totalMemory)).append("\r\n");
        builder.append("max_memory:").append(maxMemory).append("\r\n");
        builder.append("max_memory_human:").append(humanReadableByteCountBin(maxMemory)).append("\r\n");
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        builder.append("heap_memory_init:").append(heapMemoryUsage.getInit()).append("\r\n");
        builder.append("heap_memory_init_human:").append(humanReadableByteCountBin(heapMemoryUsage.getInit())).append("\r\n");
        builder.append("heap_memory_used:").append(heapMemoryUsage.getUsed()).append("\r\n");
        builder.append("heap_memory_used_human:").append(humanReadableByteCountBin(heapMemoryUsage.getUsed())).append("\r\n");
        builder.append("heap_memory_max:").append(heapMemoryUsage.getMax()).append("\r\n");
        builder.append("heap_memory_max_human:").append(humanReadableByteCountBin(heapMemoryUsage.getMax())).append("\r\n");
        builder.append("heap_memory_committed:").append(heapMemoryUsage.getCommitted()).append("\r\n");
        builder.append("heap_memory_committed_human:").append(humanReadableByteCountBin(heapMemoryUsage.getCommitted())).append("\r\n");
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        builder.append("non_heap_memory_init:").append(nonHeapMemoryUsage.getInit()).append("\r\n");
        builder.append("non_heap_memory_init_human:").append(humanReadableByteCountBin(nonHeapMemoryUsage.getInit())).append("\r\n");
        builder.append("non_heap_memory_used:").append(nonHeapMemoryUsage.getUsed()).append("\r\n");
        builder.append("non_heap_memory_used_human:").append(humanReadableByteCountBin(nonHeapMemoryUsage.getUsed())).append("\r\n");
        builder.append("non_heap_memory_max:").append(nonHeapMemoryUsage.getMax()).append("\r\n");
        builder.append("non_heap_memory_max_human:").append(humanReadableByteCountBin(nonHeapMemoryUsage.getMax())).append("\r\n");
        builder.append("non_heap_memory_committed:").append(nonHeapMemoryUsage.getCommitted()).append("\r\n");
        builder.append("non_heap_memory_committed_human:").append(humanReadableByteCountBin(nonHeapMemoryUsage.getCommitted())).append("\r\n");
        return builder.toString();
    }

    private static String getGC() {
        StringBuilder builder = new StringBuilder();
        builder.append("# GC").append("\r\n");
        if (garbageCollectorMXBeanList != null) {
            for (int i=0; i<garbageCollectorMXBeanList.size(); i++) {
                GarbageCollectorMXBean garbageCollectorMXBean = garbageCollectorMXBeanList.get(i);
                builder.append("gc").append(i).append("_name:").append(garbageCollectorMXBean.getName()).append("\r\n");
                builder.append("gc").append(i).append("_collection_count:").append(garbageCollectorMXBean.getCollectionCount()).append("\r\n");
                builder.append("gc").append(i).append("_collection_time:").append(garbageCollectorMXBean.getCollectionCount()).append("\r\n");
            }
        }
        return builder.toString();
    }

    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + "B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.2f%c", value / 1024.0, ci.current());
    }
}
