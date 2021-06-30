package com.netease.nim.camellia.redis.proxy.command.async.info;

import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncCamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncCamelliaRedisTemplateChooser;
import com.netease.nim.camellia.redis.proxy.command.async.RedisClient;
import com.netease.nim.camellia.redis.proxy.command.async.RedisClientAddr;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.monitor.RedisMonitor;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 * Created by caojiajun on 2021/6/24
 */
public class ProxyInfoUtils {

    private static final Logger logger = LoggerFactory.getLogger(ProxyInfoUtils.class);

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(8), new DefaultThreadFactory("proxy-info"));

    private static final String VERSION = "v1.0.31";
    private static int port;
    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    private static GarbageCollectorMXBean oldGC;
    private static GarbageCollectorMXBean youngGC;

    static {
        initGcMXBean();
    }

    public static void updatePort(int port) {
        ProxyInfoUtils.port = port;
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
                    } else if (section.equalsIgnoreCase("upstream-info")) {
                        builder.append(UpstreamInfoUtils.upstreamInfo(null, null, chooser)).append("\n");
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
                        builder.append(UpstreamInfoUtils.upstreamInfo(bid, bgroup, chooser)).append("\n");
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
        builder.append("available_processors:").append(osBean.getAvailableProcessors()).append("\n");
        builder.append("arch:").append(osBean.getArch()).append("\n");
        builder.append("os_name:").append(osBean.getName()).append("\n");
        builder.append("os_version:").append(osBean.getVersion()).append("\n");
        builder.append("system_load_average:").append(osBean.getSystemLoadAverage()).append("\n");
        builder.append("tcp_port:").append(port).append("\n");
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
        builder.append("total_memory:").append(totalMemory).append("\n");
        builder.append("max_memory:").append(maxMemory).append("\n");
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        builder.append("heap_memory_init:").append(heapMemoryUsage.getInit()).append("\n");
        builder.append("heap_memory_used:").append(heapMemoryUsage.getUsed()).append("\n");
        builder.append("heap_memory_max:").append(heapMemoryUsage.getMax()).append("\n");
        builder.append("heap_memory_committed:").append(heapMemoryUsage.getCommitted()).append("\n");
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        builder.append("non_heap_memory_init:").append(nonHeapMemoryUsage.getInit()).append("\n");
        builder.append("non_heap_memory_used:").append(nonHeapMemoryUsage.getUsed()).append("\n");
        builder.append("non_heap_memory_max:").append(nonHeapMemoryUsage.getMax()).append("\n");
        builder.append("non_heap_memory_committed:").append(nonHeapMemoryUsage.getCommitted()).append("\n");
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
            builder.append("\n");
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
}
