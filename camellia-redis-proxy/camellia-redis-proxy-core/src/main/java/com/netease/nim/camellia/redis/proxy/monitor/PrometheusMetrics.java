package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.monitor.model.*;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.tools.sys.CpuUsage;
import com.netease.nim.camellia.tools.sys.MemoryInfo;
import com.netease.nim.camellia.tools.sys.MemoryInfoCollector;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2023/12/22
 */
public class PrometheusMetrics {

    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private static final List<GarbageCollectorMXBean> garbageCollectorMXBeanList = ManagementFactory.getGarbageCollectorMXBeans();

    public static String metrics() {
        StringBuilder builder = new StringBuilder();

        //proxy_info
        builder.append("# HELP proxy_info Redis Proxy Info\n");
        builder.append("# TYPE proxy_info gauge\n");
        builder.append("proxy_info");
        builder.append("{");
        builder.append("proxy_version=\"").append(ProxyInfoUtils.VERSION).append("\"").append(",");
        builder.append("arch=\"").append(osBean.getArch()).append("\"").append(",");
        builder.append("os_name=\"").append(osBean.getName()).append("\"").append(",");
        builder.append("os_version=\"").append(osBean.getVersion()).append("\"").append(",");
        builder.append("system_load_average=\"").append(osBean.getSystemLoadAverage()).append("\"").append(",");
        builder.append("vm_vendor=\"").append(runtimeMXBean.getVmVendor()).append("\"").append(",");
        builder.append("vm_name=\"").append(runtimeMXBean.getVmName()).append("\"").append(",");
        builder.append("vm_version=\"").append(runtimeMXBean.getVmVersion()).append("\"").append(",");
        builder.append("jvm_info=\"").append(System.getProperties().get("java.vm.info")).append("\"").append(",");
        builder.append("java_version=\"").append(System.getProperties().get("java.version")).append("\"").append(",");
        builder.append("}").append(" 1").append("\n");

        long startTime = System.currentTimeMillis() - runtimeMXBean.getUptime();

        //uptime
        builder.append("# HELP uptime Redis Proxy Uptime\n");
        builder.append("# TYPE uptime gauge\n");
        builder.append(String.format("uptime %d\n", System.currentTimeMillis() - startTime));

        //start_time
        builder.append("# HELP start_time Redis Proxy StartTime\n");
        builder.append("# TYPE start_time gauge\n");
        builder.append(String.format("start_time %d\n", startTime));

        //memory
        builder.append("# HELP memory_info Redis Proxy Memory\n");
        builder.append("# TYPE memory_info gauge\n");
        MemoryInfo memoryInfo = MemoryInfoCollector.getMemoryInfo();
        long freeMemory = memoryInfo.getFreeMemory();
        long totalMemory = memoryInfo.getTotalMemory();
        long maxMemory = memoryInfo.getMaxMemory();
        long heapMemoryUsage = memoryInfo.getHeapMemoryUsed();
        long noneHeapMemoryUsage = memoryInfo.getNonHeapMemoryUsed();
        long nettyDirectMemory = memoryInfo.getNettyDirectMemory();
        builder.append(String.format("memory_info{type=\"free_memory\"} %d\n", freeMemory));
        builder.append(String.format("memory_info{type=\"total_memory\"} %d\n", totalMemory));
        builder.append(String.format("memory_info{type=\"max_memory\"} %d\n", maxMemory));
        builder.append(String.format("memory_info{type=\"heap_memory_usage\"} %d\n", heapMemoryUsage));
        builder.append(String.format("memory_info{type=\"no_heap_memory_usage\"} %d\n", noneHeapMemoryUsage));
        builder.append(String.format("memory_info{type=\"netty_direct_memory\"} %d\n", nettyDirectMemory));

        //cpu
        builder.append("# HELP cpu Redis Proxy Cpu\n");
        builder.append("# TYPE cpu gauge\n");
        CpuUsage cpuUsageInfo = ProxyMonitorCollector.getCpuUsageCollector().getCpuUsageInfo();
        builder.append(String.format("cpu{type=\"cpu_num\"} %d\n", cpuUsageInfo.getCpuNum()));
        builder.append(String.format("cpu{type=\"usage\"} %f\n", cpuUsageInfo.getRatio()));

        //thread
        builder.append("# HELP thread Redis Proxy Thread\n");
        builder.append("# TYPE thread gauge\n");
        builder.append(String.format("thread{type=\"boss_thread\"} %d\n", GlobalRedisProxyEnv.getBossThread()));
        builder.append(String.format("thread{type=\"work_thread\"} %d\n", GlobalRedisProxyEnv.getWorkThread()));

        //gc
        builder.append("# HELP gc Redis Proxy gc\n");
        builder.append("# TYPE gc gauge\n");
        for (GarbageCollectorMXBean bean : garbageCollectorMXBeanList) {
            builder.append(String.format("gc{name=\"%s\", type=\"count\"} %d\n", bean.getName(), bean.getCollectionCount()));
            builder.append(String.format("gc{name=\"%s\", type=\"time\"} %d\n", bean.getName(), bean.getCollectionTime()));
        }

        //client_connect
        builder.append("# HELP client_connect Redis Proxy Client Connect\n");
        builder.append("# TYPE client_connect gauge\n");
        builder.append("client_connect ").append(ChannelMonitor.connect()).append("\n");

        //qps
        builder.append("# HELP qps Redis Proxy QPS\n");
        builder.append("# TYPE qps gauge\n");
        String totalFormat = "qps{type=\"%s\"} %d\n";
        Stats stats = ProxyMonitorCollector.getStats();
        long maxReadQps = stats.getMaxReadQps();
        long maxWriteQps = stats.getMaxWriteQps();
        long maxQps = stats.getMaxQps();

        long totalReadCount = stats.getTotalReadCount();
        long totalWriteCount = stats.getTotalWriteCount();
        long count = stats.getCount();
        int intervalSeconds = stats.getIntervalSeconds() <= 0 ? 60 : stats.getIntervalSeconds();

        long readQps = totalReadCount / intervalSeconds;
        long writeQps = totalWriteCount / intervalSeconds;
        long qps = count / intervalSeconds;
        builder.append(String.format(totalFormat, "qps", qps));
        builder.append(String.format(totalFormat, "write_qps", writeQps));
        builder.append(String.format(totalFormat, "read_qps", readQps));
        builder.append(String.format(totalFormat, "max_qps", maxQps));
        builder.append(String.format(totalFormat, "max_write_qps", maxWriteQps));
        builder.append(String.format(totalFormat, "max_read_qps", maxReadQps));

        //command_qps
        builder.append("# HELP command_qps Redis Proxy Command QPS\n");
        builder.append("# TYPE command_qps gauge\n");
        List<TotalStats> totalStatsList = stats.getTotalStatsList();
        for (TotalStats totalStats : totalStatsList) {
            builder.append(String.format("command_qps{command=\"%s\"} %d\n", totalStats.getCommand(), totalStats.getCount() / intervalSeconds));
        }

        //command_spend_stats
        builder.append("# HELP command_spend_stats Redis Proxy Command Spend Stats\n");
        builder.append("# TYPE command_spend_stats gauge\n");
        List<SpendStats> spendStatsList = stats.getSpendStatsList();
        for (SpendStats spendStats : spendStatsList) {
            String command = spendStats.getCommand();
            double avg = spendStats.getAvgSpendMs();
            double max = spendStats.getMaxSpendMs();
            double p50 = spendStats.getSpendMsP50();
            double p90 = spendStats.getSpendMsP90();
            double p99 = spendStats.getSpendMsP99();
            builder.append(String.format("command_spend_stats{command=\"%s\",type=\"count\"} %d\n", command, spendStats.getCount()));
            builder.append(String.format("command_spend_stats{command=\"%s\",type=\"avg\"} %f\n", command, avg));
            builder.append(String.format("command_spend_stats{command=\"%s\",type=\"max\"} %f\n", command, max));
            builder.append(String.format("command_spend_stats{command=\"%s\",type=\"p50\"} %f\n", command, p50));
            builder.append(String.format("command_spend_stats{command=\"%s\",type=\"p90\"} %f\n", command, p90));
            builder.append(String.format("command_spend_stats{command=\"%s\",type=\"p99\"} %f\n", command, p99));
        }

        //client_connect_detail
        builder.append("# HELP client_connect_detail Redis Proxy Client Connect Detail\n");
        builder.append("# TYPE client_connect_detail gauge\n");
        List<BidBgroupConnectStats> bidBgroupConnectStats = ChannelMonitor.bidBgroupConnect();
        for (BidBgroupConnectStats bidBgroupConnectStat : bidBgroupConnectStats) {
            String tenant = bidBgroupConnectStat.getBid() + "_" + bidBgroupConnectStat.getBgroup();
            builder.append(String.format("client_connect_detail{tenant=\"%s\"} %d\n", tenant, bidBgroupConnectStat.getConnect()));
        }

        //tenant_qps
        builder.append("# HELP tenant_qps Redis Proxy Tenant QPS\n");
        builder.append("# TYPE tenant_qps gauge\n");
        List<BidBgroupStats> bidBgroupStatsList = stats.getBidBgroupStatsList();
        for (BidBgroupStats bidBgroupStats : bidBgroupStatsList) {
            String tenant = bidBgroupStats.getBid() + "_" + bidBgroupStats.getBgroup();
            builder.append(String.format("tenant_qps{tenant=\"%s\"} %d\n", tenant, bidBgroupStats.getCount() / intervalSeconds));
        }

        //tenant_command_qps
        builder.append("# HELP tenant_command_qps Redis Proxy Tenant Command QPS\n");
        builder.append("# TYPE tenant_command_qps gauge\n");
        List<DetailStats> detailStatsList = stats.getDetailStatsList();
        for (DetailStats detailStats : detailStatsList) {
            String tenant = detailStats.getBid() + "_" + detailStats.getBgroup();
            String command = detailStats.getCommand();
            builder.append(String.format("tenant_command_qps{tenant=\"%s\",command=\"%s\"} %d\n", tenant, command, detailStats.getCount() / intervalSeconds));
        }

        //tenant_command_spend_stats
        builder.append("# HELP tenant_command_spend_stats Redis Proxy Tenant Command Spend Stats\n");
        builder.append("# TYPE tenant_command_spend_stats gauge\n");
        List<BidBgroupSpendStats> bidBgroupSpendStatsList = stats.getBidBgroupSpendStatsList();
        for (BidBgroupSpendStats bidBgroupSpendStats : bidBgroupSpendStatsList) {
            String tenant = bidBgroupSpendStats.getBid() + "_" + bidBgroupSpendStats.getBgroup();
            long count1 = bidBgroupSpendStats.getCount();
            String command = bidBgroupSpendStats.getCommand();
            double avg = bidBgroupSpendStats.getAvgSpendMs();
            double p50 = bidBgroupSpendStats.getSpendMsP50();
            double p90 = bidBgroupSpendStats.getSpendMsP90();
            double p99 = bidBgroupSpendStats.getSpendMsP99();
            double max = bidBgroupSpendStats.getMaxSpendMs();
            builder.append(String.format("tenant_command_spend_stats{tenant=\"%s\",command=\"%s\",type=\"count\"} %d\n", tenant, command, count1));
            builder.append(String.format("tenant_command_spend_stats{tenant=\"%s\",command=\"%s\",type=\"avg\"} %f\n", tenant, command, avg));
            builder.append(String.format("tenant_command_spend_stats{tenant=\"%s\",command=\"%s\",type=\"max\"} %f\n", tenant, command, max));
            builder.append(String.format("tenant_command_spend_stats{tenant=\"%s\",command=\"%s\",type=\"p50\"} %f\n", tenant, command, p50));
            builder.append(String.format("tenant_command_spend_stats{tenant=\"%s\",command=\"%s\",type=\"p90\"} %f\n", tenant,  command, p90));
            builder.append(String.format("tenant_command_spend_stats{tenant=\"%s\",command=\"%s\",type=\"p99\"} %f\n", tenant, command, p99));
        }

        //proxy_route_conf
        builder.append("# HELP proxy_route_conf Redis Proxy Route Conf\n");
        builder.append("# TYPE proxy_route_conf gauge\n");
        List<RouteConf> routeConfList = stats.getRouteConfList();
        for (RouteConf routeConf : routeConfList) {
            String tenant = routeConf.getBid() + "_" + routeConf.getBgroup();
            String resourceTable = routeConf.getResourceTable();
            resourceTable = resourceTable.replaceAll("\"", "'");
            builder.append(String.format("proxy_route_conf{tenant=\"%s\", route=\"%s\"} 1\n", tenant, resourceTable));
        }

        //upstream_redis_connect
        RedisConnectStats redisConnectStats = stats.getRedisConnectStats();
        builder.append("# HELP upstream_redis_connect Redis Proxy Upstream Redis Connect\n");
        builder.append("# TYPE upstream_redis_connect gauge\n");
        builder.append("upstream_redis_connect ").append(redisConnectStats.getConnectCount()).append("\n");

        //upstream_redis_connect_detail
        builder.append("# HELP upstream_redis_connect_detail Redis Proxy Upstream Redis Connect Detail\n");
        builder.append("# TYPE upstream_redis_connect_detail gauge\n");
        List<RedisConnectStats.Detail> detailList = redisConnectStats.getDetailList();
        for (RedisConnectStats.Detail detail : detailList) {
            builder.append(String.format("upstream_redis_connect_detail{upstream=\"%s\"} %d\n", detail.getAddr(), detail.getConnectCount()));
        }


        //upstream_redis_qps
        builder.append("# HELP upstream_redis_qps Redis Proxy Upstream Redis QPS\n");
        builder.append("# TYPE upstream_redis_qps gauge\n");
        List<ResourceStats> resourceStatsList = stats.getResourceStatsList();
        for (ResourceStats resourceStats : resourceStatsList) {
            builder.append(String.format("upstream_redis_qps{upstream=\"%s\"} %d\n", resourceStats.getResource(), resourceStats.getCount() / intervalSeconds));
        }

        //upstream_redis_spend_stats
        builder.append("# HELP upstream_redis_spend_stats Redis Proxy Upstream Redis Spend Stats\n");
        builder.append("# TYPE upstream_redis_spend_stats gauge\n");
        List<UpstreamRedisSpendStats> upstreamRedisSpendStatsList = stats.getUpstreamRedisSpendStatsList();
        for (UpstreamRedisSpendStats upstreamRedisSpendStats : upstreamRedisSpendStatsList) {
            long count1 = upstreamRedisSpendStats.getCount();
            String upstream = upstreamRedisSpendStats.getAddr();
            double avg = upstreamRedisSpendStats.getAvgSpendMs();
            double p50 = upstreamRedisSpendStats.getSpendMsP50();
            double p90 = upstreamRedisSpendStats.getSpendMsP90();
            double p99 = upstreamRedisSpendStats.getSpendMsP99();
            double max = upstreamRedisSpendStats.getMaxSpendMs();
            builder.append(String.format("upstream_redis_spend_stats{upstream=\"%s\", type=\"count\"} %d\n", upstream, count1));
            builder.append(String.format("upstream_redis_spend_stats{upstream=\"%s\", type=\"avg\"} %f\n", upstream, avg));
            builder.append(String.format("upstream_redis_spend_stats{upstream=\"%s\", type=\"max\"} %f\n", upstream, max));
            builder.append(String.format("upstream_redis_spend_stats{upstream=\"%s\", type=\"p50\"} %f\n", upstream, p50));
            builder.append(String.format("upstream_redis_spend_stats{upstream=\"%s\", type=\"p90\"} %f\n", upstream, p90));
            builder.append(String.format("upstream_redis_spend_stats{upstream=\"%s\", type=\"p99\"} %f\n", upstream, p99));
        }

        //client_fail
        builder.append("# HELP client_fail Redis Proxy Fail\n");
        builder.append("# TYPE client_fail gauge\n");
        Map<String, Long> failMap = stats.getFailMap();
        for (Map.Entry<String, Long> entry : failMap.entrySet()) {
            builder.append(String.format("client_fail{reason=\"%s\",} %d\n", entry.getKey(), entry.getValue()));
        }

        //upstream_fail
        builder.append("# HELP upstream_fail Redis Proxy Upstream Fail\n");
        builder.append("# TYPE upstream_fail gauge\n");
        List<UpstreamFailStats> upstreamFailStatsList = stats.getUpstreamFailStatsList();
        for (UpstreamFailStats upstreamFailStats : upstreamFailStatsList) {
            String upstream = upstreamFailStats.getResource();
            String command = upstreamFailStats.getCommand();
            String msg = upstreamFailStats.getMsg();
            long count1 = upstreamFailStats.getCount();
            builder.append(String.format("upstream_fail{upstream=\"%s\",command=\"%s\",msg=\"%s\"} %d\n", upstream, command, msg, count1));
        }

        //slow_command
        builder.append("# HELP slow_command Redis Proxy Slow Command\n");
        builder.append("# TYPE slow_command gauge\n");
        List<SlowCommandStats> slowCommandStatsList = stats.getSlowCommandStatsList();
        for (SlowCommandStats slowCommandStats : slowCommandStatsList) {
            String tenant = slowCommandStats.getBid() + "_" + slowCommandStats.getBgroup();
            String command = slowCommandStats.getCommand();
            String keys = slowCommandStats.getKeys();
            double spendMillis = slowCommandStats.getSpendMillis();
            builder.append(String.format("slow_command{tenant=\"%s\",command=\"%s\",keys=\"%s\"} %f\n", tenant, command, keys, spendMillis));
        }

        List<BigKeyStats> bigKeyStatsList = stats.getBigKeyStatsList();

        //string_big_key
        builder.append("# HELP string_big_key Redis Proxy Big Key\n");
        builder.append("# TYPE string_big_key gauge\n");
        for (BigKeyStats bigKeyStats : bigKeyStatsList) {
            String commandType = bigKeyStats.getCommandType();
            if (!commandType.equalsIgnoreCase("string")) continue;
            String tenant = bigKeyStats.getBid() + "_" + bigKeyStats.getBgroup();
            String command = bigKeyStats.getCommand();
            String key = bigKeyStats.getKey();
            long size = bigKeyStats.getSize();
            builder.append(String.format("string_big_key{tenant=\"%s\",commandType=\"%s\",command=\"%s\",key=\"%s\"} %d\n", tenant, commandType, command, key, size));
        }

        //collection_big_key
        builder.append("# HELP collection_big_key Redis Proxy Big Key\n");
        builder.append("# TYPE collection_big_key gauge\n");
        for (BigKeyStats bigKeyStats : bigKeyStatsList) {
            String commandType = bigKeyStats.getCommandType();
            if (commandType.equalsIgnoreCase("string")) continue;
            String tenant = bigKeyStats.getBid() + "_" + bigKeyStats.getBgroup();
            String command = bigKeyStats.getCommand();
            String key = bigKeyStats.getKey();
            long size = bigKeyStats.getSize();
            builder.append(String.format("collection_big_key{tenant=\"%s\",commandType=\"%s\",command=\"%s\",key=\"%s\"} %d\n", tenant, commandType, command, key, size));
        }

        //hot_key
        builder.append("# HELP hot_key Redis Proxy Hot Key\n");
        builder.append("# TYPE hot_key gauge\n");
        List<HotKeyStats> hotKeyStatsList = stats.getHotKeyStatsList();
        for (HotKeyStats hotKeyStats : hotKeyStatsList) {
            String tenant = hotKeyStats.getBid() + "_" + hotKeyStats.getBgroup();
            String key = hotKeyStats.getKey();
            long count1 = hotKeyStats.getCount();
            long max = hotKeyStats.getMax();
            builder.append(String.format("hot_key{tenant=\"%s\",key=\"%s\",type=\"qps\"} %d\n", tenant, key, max));
            builder.append(String.format("hot_key{tenant=\"%s\",key=\"%s\",type=\"count\"} %d\n", tenant, key, count1));
        }

        //hot_key_cache_hit
        builder.append("# HELP hot_key_cache_hit Redis Proxy Hot Key Cache Hit\n");
        builder.append("# TYPE hot_key_cache_hit gauge\n");
        List<HotKeyCacheStats> hotKeyCacheStatsList = stats.getHotKeyCacheStatsList();
        for (HotKeyCacheStats hotKeyCacheStats : hotKeyCacheStatsList) {
            String tenant = hotKeyCacheStats.getBid() + "_" + hotKeyCacheStats.getBgroup();
            String key = hotKeyCacheStats.getKey();
            long hitCount = hotKeyCacheStats.getHitCount();
            builder.append(String.format("hot_key_cache_hit{tenant=\"%s\",key=\"%s\"} %d\n", tenant, key, hitCount));
        }

        return builder.toString();
    }
}
