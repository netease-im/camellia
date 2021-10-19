package com.netease.nim.camellia.redis.proxy.command.async.info;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncCamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncCamelliaRedisTemplateChooser;
import com.netease.nim.camellia.redis.proxy.command.async.RedisClient;
import com.netease.nim.camellia.redis.proxy.command.async.RedisClientAddr;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.monitor.RedisMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.Stats;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
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

    private static final String VERSION = "v1.0.39";
    private static int port;
    private static int consolePort;
    private static int bossThread;
    private static int workThread;
    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    private static GarbageCollectorMXBean oldGC;
    private static GarbageCollectorMXBean youngGC;

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

    private static AsyncCamelliaRedisTemplateChooser chooser;

    static {
        initGcMXBean();
    }

    public static int getPort() {
        return port;
    }

    public static int getConsolePort() {
        return consolePort;
    }

    public static void updatePort(int port) {
        ProxyInfoUtils.port = port;
    }

    public static void updateAsyncCamelliaRedisTemplateChooser(AsyncCamelliaRedisTemplateChooser chooser) {
        ProxyInfoUtils.chooser = chooser;
    }

    public static AsyncCamelliaRedisTemplateChooser getAsyncCamelliaRedisTemplateChooser() {
        return chooser;
    }

    public static void updateConsolePort(int consolePort) {
        ProxyInfoUtils.consolePort = consolePort;
    }

    public static void updateThread(int bossThread, int workThread) {
        ProxyInfoUtils.bossThread = bossThread;
        ProxyInfoUtils.workThread = workThread;
    }

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
                    return UpstreamInfoUtils.upstreamInfo(null, null, chooser, parseJson);
                }
                try {
                    Long.parseLong(bid);
                } catch (NumberFormatException e) {
                    return parseResponse(ErrorReply.SYNTAX_ERROR, parseJson);
                }
                return UpstreamInfoUtils.upstreamInfo(Long.parseLong(bid), bgroup, chooser, parseJson);
            } else {
                Reply reply = generateInfoReply(new Command(new byte[][]{RedisCommand.INFO.raw(), section.getBytes(StandardCharsets.UTF_8)}), chooser);
                return parseResponse(reply, parseJson);
            }
        } else {
            Reply reply = generateInfoReply(new Command(new byte[][]{RedisCommand.INFO.raw()}), chooser);
            return parseResponse(reply, parseJson);
        }
    }

    private static String parseResponse(Reply reply, boolean parseJson) {
        String string = replyToString(reply);
        if (!parseJson) return string;
        String[] split = string.split("\n");
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
                    item.put(itemKey, itemValue);
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
                builder.append(getServer()).append("\n");
                builder.append(getClients()).append("\n");
                builder.append(getRoutes()).append("\n");
                builder.append(getUpstream()).append("\n");
                builder.append(getMemory()).append("\n");
                builder.append(getGC()).append("\n");
                builder.append(getStats()).append("\n");
            } else {
                if (objects.length == 2) {
                    String section = Utils.bytesToString(objects[1]);
                    if (section.equalsIgnoreCase("server")) {
                        builder.append(getServer()).append("\n");
                    } else if (section.equalsIgnoreCase("clients")) {
                        builder.append(getClients()).append("\n");
                    } else if (section.equalsIgnoreCase("route")) {
                        builder.append(getRoutes()).append("\n");
                    } else if (section.equalsIgnoreCase("upstream")) {
                        builder.append(getUpstream()).append("\n");
                    } else if (section.equalsIgnoreCase("memory")) {
                        builder.append(getMemory()).append("\n");
                    } else if (section.equalsIgnoreCase("gc")) {
                        builder.append(getGC()).append("\n");
                    } else if (section.equalsIgnoreCase("stats")) {
                        builder.append(getStats()).append("\n");
                    } else if (section.equalsIgnoreCase("upstream-info")) {
                        builder.append(UpstreamInfoUtils.upstreamInfo(null, null, chooser, false)).append("\n");
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
                        builder.append(UpstreamInfoUtils.upstreamInfo(bid, bgroup, chooser, false)).append("\n");
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
        builder.append("# Server").append("\n");
        builder.append("camellia_redis_proxy_version:" + VERSION).append("\n");
        builder.append("redis_version:6.2.5").append("\n");//spring actuator默认会使用info命令返回的redis_version字段来做健康检查，这里直接返回一个固定的版本号
        builder.append("available_processors:").append(osBean.getAvailableProcessors()).append("\n");
        builder.append("netty_boss_thread:").append(bossThread).append("\n");
        builder.append("netty_work_thread:").append(workThread).append("\n");
        builder.append("arch:").append(osBean.getArch()).append("\n");
        builder.append("os_name:").append(osBean.getName()).append("\n");
        builder.append("os_version:").append(osBean.getVersion()).append("\n");
        builder.append("system_load_average:").append(osBean.getSystemLoadAverage()).append("\n");
        builder.append("tcp_port:").append(port).append("\n");
        builder.append("http_console_port:").append(consolePort).append("\n");
        long uptime = runtimeMXBean.getUptime();
        long uptimeInSeconds = uptime / 1000L;
        long uptimeInDays = uptime / (1000L * 60 * 60 * 24);
        builder.append("uptime_in_seconds:").append(uptimeInSeconds).append("\n");
        builder.append("uptime_in_days:").append(uptimeInDays).append("\n");
        builder.append("vm_vendor:").append(runtimeMXBean.getVmVendor()).append("\n");
        builder.append("vm_name:").append(runtimeMXBean.getVmName()).append("\n");
        builder.append("vm_version:").append(runtimeMXBean.getVmVersion()).append("\n");
        builder.append("jvm_info:").append(System.getProperties().get("java.vm.info")).append("\n");
        builder.append("java_version:").append(System.getProperties().get("java.version")).append("\n");
        return builder.toString();
    }

    private static String getClients() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Clients").append("\n");
        builder.append("connect_clients:").append(RedisMonitor.getClientConnects()).append("\n");
        return builder.toString();
    }

    private static String getStats() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Stats").append("\n");
        builder.append("commands_count:").append(commandsCount).append("\n");
        builder.append("read_commands_count:").append(readCommandsCount).append("\n");
        builder.append("write_commands_count:").append(writeCommandsCount).append("\n");
        builder.append("avg_commands_qps:").append(String.format("%.2f", avgCommandsQps)).append("\n");
        builder.append("avg_read_commands_qps:").append(String.format("%.2f", avgReadCommandsQps)).append("\n");
        builder.append("avg_write_commands_qps:").append(String.format("%.2f", avgWriteCommandsQps)).append("\n");
        builder.append("monitor_interval_seconds:").append(monitorIntervalSeconds).append("\n");
        builder.append("last_commands_qps:").append(String.format("%.2f", lastCommandQps)).append("\n");
        builder.append("last_read_commands_qps:").append(String.format("%.2f", lastReadCommandQps)).append("\n");
        builder.append("last_write_commands_qps:").append(String.format("%.2f", lastWriteCommandQps)).append("\n");
        return builder.toString();
    }

    private static String getRoutes() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Route").append("\n");
        ConcurrentHashMap<String, AsyncCamelliaRedisTemplate> templateMap = RedisMonitor.getTemplateMap();
        builder.append("route_nums:").append(templateMap.size()).append("\n");
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
            builder.append("route_conf_").append(bid == null ? "default" : bid).append("_").append(bgroup == null ? "default" : bgroup).append(":").append(routeConf).append("\n");
        }
        return builder.toString();
    }

    private static String getUpstream() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Upstream").append("\n");
        ConcurrentHashMap<RedisClientAddr, ConcurrentHashMap<String, RedisClient>> redisClientMap = RedisMonitor.getRedisClientMap();
        int upstreamRedisNums = 0;
        for (Map.Entry<RedisClientAddr, ConcurrentHashMap<String, RedisClient>> entry : redisClientMap.entrySet()) {
            upstreamRedisNums += entry.getValue().size();
        }
        builder.append("upstream_redis_nums:").append(upstreamRedisNums).append("\n");
        for (Map.Entry<RedisClientAddr, ConcurrentHashMap<String, RedisClient>> entry : redisClientMap.entrySet()) {
            builder.append("upstream_redis_nums").append("[").append(PasswordMaskUtils.maskAddr(entry.getKey().getUrl())).append("]").append(":").append(entry.getValue().size()).append("\n");
        }
        return builder.toString();
    }

    private static String getMemory() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Memory").append("\n");
        long freeMemory = Runtime.getRuntime().freeMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        builder.append("free_memory:").append(freeMemory).append("\n");
        builder.append("free_memory_human:").append(humanReadableByteCountBin(freeMemory)).append("\n");
        builder.append("total_memory:").append(totalMemory).append("\n");
        builder.append("total_memory_human:").append(humanReadableByteCountBin(totalMemory)).append("\n");
        builder.append("max_memory:").append(maxMemory).append("\n");
        builder.append("max_memory_human:").append(humanReadableByteCountBin(maxMemory)).append("\n");
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        builder.append("heap_memory_init:").append(heapMemoryUsage.getInit()).append("\n");
        builder.append("heap_memory_init_human:").append(humanReadableByteCountBin(heapMemoryUsage.getInit())).append("\n");
        builder.append("heap_memory_used:").append(heapMemoryUsage.getUsed()).append("\n");
        builder.append("heap_memory_used_human:").append(humanReadableByteCountBin(heapMemoryUsage.getUsed())).append("\n");
        builder.append("heap_memory_max:").append(heapMemoryUsage.getMax()).append("\n");
        builder.append("heap_memory_max_human:").append(humanReadableByteCountBin(heapMemoryUsage.getMax())).append("\n");
        builder.append("heap_memory_committed:").append(heapMemoryUsage.getCommitted()).append("\n");
        builder.append("heap_memory_committed_human:").append(humanReadableByteCountBin(heapMemoryUsage.getCommitted())).append("\n");
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        builder.append("non_heap_memory_init:").append(nonHeapMemoryUsage.getInit()).append("\n");
        builder.append("non_heap_memory_init_human:").append(humanReadableByteCountBin(nonHeapMemoryUsage.getInit())).append("\n");
        builder.append("non_heap_memory_used:").append(nonHeapMemoryUsage.getUsed()).append("\n");
        builder.append("non_heap_memory_used_human:").append(humanReadableByteCountBin(nonHeapMemoryUsage.getUsed())).append("\n");
        builder.append("non_heap_memory_max:").append(nonHeapMemoryUsage.getMax()).append("\n");
        builder.append("non_heap_memory_max_human:").append(humanReadableByteCountBin(nonHeapMemoryUsage.getMax())).append("\n");
        builder.append("non_heap_memory_committed:").append(nonHeapMemoryUsage.getCommitted()).append("\n");
        builder.append("non_heap_memory_committed_human:").append(humanReadableByteCountBin(nonHeapMemoryUsage.getCommitted())).append("\n");
        return builder.toString();
    }

    private static String getGC() {
        StringBuilder builder = new StringBuilder();
        if (oldGC != null && youngGC != null) {
            builder.append("# GC").append("\n");
            builder.append("young_gc_name:").append(youngGC.getName()).append("\n");
            builder.append("young_gc_collection_count:").append(youngGC.getCollectionCount()).append("\n");
            builder.append("young_gc_collection_time:").append(youngGC.getCollectionTime()).append("\n");
            builder.append("old_gc_name:").append(oldGC.getName()).append("\n");
            builder.append("old_gc_collection_count:").append(oldGC.getCollectionCount()).append("\n");
            builder.append("old_gc_collection_time:").append(oldGC.getCollectionTime()).append("\n");
        }
        return builder.toString();
    }

    private static void initGcMXBean() {
        try {
            List<GarbageCollectorMXBean> list = ManagementFactory.getGarbageCollectorMXBeans();

            Set<String> oldGcNames = new HashSet<>();
            oldGcNames.add("ConcurrentMarkSweep");
            oldGcNames.add("MarkSweepCompact");
            oldGcNames.add("PS MarkSweep");
            oldGcNames.add("G1 Old Generation");
            oldGcNames.add("Garbage collection optimized for short pausetimes Old Collector");
            oldGcNames.add("Garbage collection optimized for throughput Old Collector");
            oldGcNames.add("Garbage collection optimized for deterministic pausetimes Old Collector");
            String oldGcNameConf = ProxyDynamicConf.getString("redis.info.old.gc.names", "");
            if (oldGcNameConf != null && oldGcNameConf.trim().length() > 0) {
                String[] split = oldGcNameConf.split(",");
                oldGcNames.addAll(Arrays.asList(split));
            }

            Set<String> youngGcNames = new HashSet<>();
            youngGcNames.add("ParNew");
            youngGcNames.add("Copy");
            youngGcNames.add("PS Scavenge");
            youngGcNames.add("G1 Young Generation");
            youngGcNames.add("Garbage collection optimized for short pausetimes Young Collector");
            youngGcNames.add("Garbage collection optimized for throughput Young Collector");
            youngGcNames.add("Garbage collection optimized for deterministic pausetimes Young Collector");
            String youngGcNameConf = ProxyDynamicConf.getString("redis.info.young.gc.names", "");
            if (youngGcNameConf != null && youngGcNameConf.trim().length() > 0) {
                String[] split = youngGcNameConf.split(",");
                youngGcNames.addAll(Arrays.asList(split));
            }

            for (GarbageCollectorMXBean bean : list) {
                if (oldGcNames.contains(bean.getName())) {
                    oldGC = bean;
                }
                if (youngGcNames.contains(bean.getName())) {
                    youngGC = bean;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
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
