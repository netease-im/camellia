package com.netease.nim.camellia.hot.key.server.monitor;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.constant.CamelliaVersion;
import com.netease.nim.camellia.hot.key.server.callback.HotKeyInfo;
import com.netease.nim.camellia.tools.sys.CpuUsage;
import com.netease.nim.camellia.tools.sys.MemoryInfo;
import com.netease.nim.camellia.tools.sys.MemoryInfoCollector;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Set;


public class PrometheusMetrics {

    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private static final List<GarbageCollectorMXBean> garbageCollectorMXBeanList = ManagementFactory.getGarbageCollectorMXBeans();

    public static String metrics() {
        StringBuilder builder = new StringBuilder();

        //info
        builder.append("# HELP info Hot Key Server Info\n");
        builder.append("# TYPE info gauge\n");
        builder.append("info");
        builder.append("{");
        builder.append("camellia_version=\"").append(CamelliaVersion.version).append("\"").append(",");
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
        builder.append("# HELP uptime Hot Key Server Uptime\n");
        builder.append("# TYPE uptime gauge\n");
        builder.append(String.format("uptime %d\n", System.currentTimeMillis() - startTime));

        //start_time
        builder.append("# HELP start_time Hot Key Server StartTime\n");
        builder.append("# TYPE start_time gauge\n");
        builder.append(String.format("start_time %d\n", startTime));

        //memory
        builder.append("# HELP memory_info Hot Key Server Memory\n");
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
        builder.append("# HELP cpu Hot Key Server Cpu\n");
        builder.append("# TYPE cpu gauge\n");
        CpuUsage cpuUsageInfo = HotKeyServerMonitorCollector.getCpuUsageCollector().getCpuUsageInfo();
        builder.append(String.format("cpu{type=\"cpu_num\"} %d\n", cpuUsageInfo.getCpuNum()));
        builder.append(String.format("cpu{type=\"usage\"} %f\n", cpuUsageInfo.getRatio()));

        //gc
        builder.append("# HELP gc Hot Key Server gc\n");
        builder.append("# TYPE gc gauge\n");
        for (GarbageCollectorMXBean bean : garbageCollectorMXBeanList) {
            builder.append(String.format("gc{name=\"%s\", type=\"count\"} %d\n", bean.getName(), bean.getCollectionCount()));
            builder.append(String.format("gc{name=\"%s\", type=\"time\"} %d\n", bean.getName(), bean.getCollectionTime()));
        }

        HotKeyServerStats hotKeyServerStats = HotKeyServerMonitorCollector.getHotKeyServerStats();

        //thread
        builder.append("# HELP thread Hot Key Server Thread\n");
        builder.append("# TYPE thread gauge\n");
        builder.append(String.format("thread %d\n", hotKeyServerStats.getQueueStats().getQueueNum()));

        //client_connect
        builder.append("# HELP client_connect Hot Key Server Client Connect\n");
        builder.append("# TYPE client_connect gauge\n");
        long connectCount = hotKeyServerStats.getConnectCount();
        builder.append(String.format("client_connect %d\n", connectCount));

        //queue
        builder.append("# HELP queue Hot Key Server Queue\n");
        builder.append("# TYPE queue gauge\n");
        List<QueueStats.Stats> statsList = hotKeyServerStats.getQueueStats().getStatsList();
        for (QueueStats.Stats stats : statsList) {
            builder.append(String.format("queue{name=\"%s\", type=\"pending\"} %d\n", stats.getId(), stats.getPendingSize()));
            builder.append(String.format("queue{name=\"%s\", type=\"discard\"} %d\n", stats.getId(), stats.getDiscardCount()));
        }

        TrafficStats trafficStats = hotKeyServerStats.getTrafficStats();

        //qps
        builder.append("# HELP qps Hot Key Server QPS\n");
        builder.append("# TYPE qps gauge\n");
        builder.append(String.format("qps %d\n", trafficStats.getTotal() / hotKeyServerStats.getMonitorIntervalSeconds()));

        //qps_detail
        builder.append("# HELP qps_detail Hot Key Server QPS Detail\n");
        builder.append("# TYPE qps_detail gauge\n");
        List<TrafficStats.Stats> trafficStatsStatsList = trafficStats.getStatsList();
        for (TrafficStats.Stats stats : trafficStatsStatsList) {
            builder.append(String.format("qps_detail{namespace=\"%s\", type=\"%s\"} %d\n",
                    stats.getNamespace(), stats.getType().name(), stats.getCount() / hotKeyServerStats.getMonitorIntervalSeconds()));
        }

        //hot_key
        builder.append("# HELP hot_key Hot Key Server Hot Key\n");
        builder.append("# TYPE hot_key gauge\n");
        List<HotKeyInfo> hotKeyInfoList = hotKeyServerStats.getHotKeyInfoList();
        for (HotKeyInfo hotKeyInfo : hotKeyInfoList) {
            String rule = JSONObject.toJSONString(hotKeyInfo.getRule()).replaceAll("\"", "'");
            Set<String> sourceSet = hotKeyInfo.getSourceSet();
            StringBuilder source = new StringBuilder();
            if (sourceSet != null) {
                for (String str : sourceSet) {
                    source.append(str).append("|");
                }
                if (source.length() > 0) {
                    source.deleteCharAt(source.length() - 1);
                }
            }
            builder.append(String.format("hot_key{namespace=\"%s\", key=\"%s\", action=\"%s\", rule=\"%s\", source=\"%s\"} %d\n",
                    hotKeyInfo.getNamespace(), hotKeyInfo.getKey(), hotKeyInfo.getAction(), rule, source, hotKeyInfo.getCount()));
        }
        return builder.toString();
    }
}
