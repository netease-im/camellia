package com.netease.nim.camellia.id.gen.monitor;

import com.netease.nim.camellia.core.constant.CamelliaVersion;
import com.netease.nim.camellia.tools.sys.CpuUsage;
import com.netease.nim.camellia.tools.sys.MemoryInfo;
import com.netease.nim.camellia.tools.sys.MemoryInfoCollector;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.List;


public class PrometheusMetrics {

    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private static final List<GarbageCollectorMXBean> garbageCollectorMXBeanList = ManagementFactory.getGarbageCollectorMXBeans();

    public static String metrics() {
        StringBuilder builder = new StringBuilder();

        //info
        builder.append("# HELP info Id Gen Server Info\n");
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
        builder.append("# HELP uptime Id Gen Server Uptime\n");
        builder.append("# TYPE uptime gauge\n");
        builder.append(String.format("uptime %d\n", System.currentTimeMillis() - startTime));

        //start_time
        builder.append("# HELP start_time Id Gen Server StartTime\n");
        builder.append("# TYPE start_time gauge\n");
        builder.append(String.format("start_time %d\n", startTime));

        //memory
        builder.append("# HELP memory_info Id Gen Server Memory\n");
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
        builder.append("# HELP cpu Id Gen Server Cpu\n");
        builder.append("# TYPE cpu gauge\n");
        CpuUsage cpuUsageInfo = IdGenMonitor.getCpuUsageCollector().getCpuUsageInfo();
        builder.append(String.format("cpu{type=\"cpu_num\"} %d\n", cpuUsageInfo.getCpuNum()));
        builder.append(String.format("cpu{type=\"usage\"} %f\n", cpuUsageInfo.getRatio()));

        //gc
        builder.append("# HELP gc Id Gen Server gc\n");
        builder.append("# TYPE gc gauge\n");
        for (GarbageCollectorMXBean bean : garbageCollectorMXBeanList) {
            builder.append(String.format("gc{name=\"%s\", type=\"count\"} %d\n", bean.getName(), bean.getCollectionCount()));
            builder.append(String.format("gc{name=\"%s\", type=\"time\"} %d\n", bean.getName(), bean.getCollectionTime()));
        }

        //request
        builder.append("# HELP request Id Gen Server request\n");
        builder.append("# TYPE request gauge\n");
        Stats stats = IdGenMonitor.getStats();
        List<Stats.UriStats> statsList = stats.getStatsList();
        for (Stats.UriStats uriStats : statsList) {
            String uri = uriStats.getUri();
            int code = uriStats.getCode();
            builder.append(String.format("request{uri=\"%s\", code=\"%s\", type=\"count\"} %d\n", uri, code, uriStats.getCount()));
            builder.append(String.format("request{uri=\"%s\", code=\"%s\", type=\"spendAvg\"} %d\n", uri, code, uriStats.getSpendAvg()));
            builder.append(String.format("request{uri=\"%s\", code=\"%s\", type=\"spendMax\"} %d\n", uri, code, uriStats.getSpendMax()));
            builder.append(String.format("request{uri=\"%s\", code=\"%s\", type=\"spendP50\"} %d\n", uri, code, uriStats.getSpendP50()));
            builder.append(String.format("request{uri=\"%s\", code=\"%s\", type=\"spendP90\"} %d\n", uri, code, uriStats.getSpendP90()));
            builder.append(String.format("request{uri=\"%s\", code=\"%s\", type=\"spendP99\"} %d\n", uri, code, uriStats.getSpendP99()));
        }

        return builder.toString();
    }
}
