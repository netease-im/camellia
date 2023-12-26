package com.netease.nim.camellia.delayqueue.server.springboot;

import com.netease.nim.camellia.core.constant.CamelliaVersion;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueMonitor;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueMonitorData;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueServer;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueTopicInfo;
import com.netease.nim.camellia.tools.sys.CpuUsage;
import com.netease.nim.camellia.tools.sys.MemoryInfo;
import com.netease.nim.camellia.tools.sys.MemoryInfoCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.List;

/**
 * Created by caojiajun on 2023/12/25
 */
@RestController
public class MetricController {

    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private static final List<GarbageCollectorMXBean> garbageCollectorMXBeanList = ManagementFactory.getGarbageCollectorMXBeans();

    @Autowired
    private CamelliaDelayQueueServer server;

    @GetMapping(value = "/metrics", produces = "text/plain;charset=UTF-8")
    public String metrics() {
        StringBuilder builder = new StringBuilder();

        //proxy_info
        builder.append("# HELP info Delay Queue Info\n");
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
        builder.append("# HELP uptime Delay Queue Uptime\n");
        builder.append("# TYPE uptime gauge\n");
        builder.append(String.format("uptime %d\n", System.currentTimeMillis() - startTime));

        //start_time
        builder.append("# HELP start_time Delay Queue StartTime\n");
        builder.append("# TYPE start_time gauge\n");
        builder.append(String.format("start_time %d\n", startTime));

        //memory
        builder.append("# HELP memory_info Delay Queue Memory\n");
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
        builder.append("# HELP cpu Delay Queue Cpu\n");
        builder.append("# TYPE cpu gauge\n");
        CpuUsage cpuUsageInfo = CamelliaDelayQueueMonitor.getCpuUsageCollector().getCpuUsageInfo();
        builder.append(String.format("cpu{type=\"cpu_num\"} %d\n", cpuUsageInfo.getCpuNum()));
        builder.append(String.format("cpu{type=\"usage\"} %f\n", cpuUsageInfo.getRatio()));

        //gc
        builder.append("# HELP gc Delay Queue gc\n");
        builder.append("# TYPE gc gauge\n");
        for (GarbageCollectorMXBean bean : garbageCollectorMXBeanList) {
            builder.append(String.format("gc{name=\"%s\", type=\"count\"} %d\n", bean.getName(), bean.getCollectionCount()));
            builder.append(String.format("gc{name=\"%s\", type=\"time\"} %d\n", bean.getName(), bean.getCollectionTime()));
        }

        CamelliaDelayQueueMonitorData monitorData = CamelliaDelayQueueMonitor.getMonitorData();

        //request
        builder.append("# HELP request Delay Queue Request\n");
        builder.append("# TYPE request gauge\n");
        List<CamelliaDelayQueueMonitorData.RequestStats> requestStatsList = monitorData.getRequestStatsList();
        for (CamelliaDelayQueueMonitorData.RequestStats requestStats : requestStatsList) {
            String topic = requestStats.getTopic();
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "sendMsg", requestStats.getSendMsg()));
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "pullMsg", requestStats.getPullMsg()));
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "deleteMsg", requestStats.getDeleteMsg()));
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "ackMsg", requestStats.getAckMsg()));
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "getMsg", requestStats.getGetMsg()));
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "triggerMsgReady", requestStats.getTriggerMsgReady()));
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "triggerMsgTimeout", requestStats.getTriggerMsgTimeout()));
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "triggerMsgEndLife", requestStats.getTriggerMsgEndLife()));
        }

        //pull_msg_time_gap
        builder.append("# HELP pull_msg_time_gap Delay Queue Pull Msg Time Gap\n");
        builder.append("# TYPE pull_msg_time_gap gauge\n");
        List<CamelliaDelayQueueMonitorData.TimeGapStats> pullMsgTimeGapStatsList = monitorData.getPullMsgTimeGapStatsList();
        for (CamelliaDelayQueueMonitorData.TimeGapStats timeGapStats : pullMsgTimeGapStatsList) {
            String topic = timeGapStats.getTopic();
            builder.append(String.format("pull_msg_time_gap{topic=\"%s\", type=\"%s\"} %d\n", topic, "count", timeGapStats.getCount()));
            builder.append(String.format("pull_msg_time_gap{topic=\"%s\", type=\"%s\"} %f\n", topic, "avg", timeGapStats.getAvg()));
            builder.append(String.format("pull_msg_time_gap{topic=\"%s\", type=\"%s\"} %d\n", topic, "max", timeGapStats.getMax()));
        }

        //ready_queue_time_gap
        builder.append("# HELP ready_queue_time_gap Delay Queue Ready Queue Time Gap\n");
        builder.append("# TYPE ready_queue_time_gap gauge\n");
        List<CamelliaDelayQueueMonitorData.TimeGapStats> readyQueueTimeGapStatsList = monitorData.getReadyQueueTimeGapStatsList();
        for (CamelliaDelayQueueMonitorData.TimeGapStats timeGapStats : readyQueueTimeGapStatsList) {
            String topic = timeGapStats.getTopic();
            builder.append(String.format("ready_queue_time_gap{topic=\"%s\", type=\"%s\"} %d\n", topic, "count", timeGapStats.getCount()));
            builder.append(String.format("ready_queue_time_gap{topic=\"%s\", type=\"%s\"} %f\n", topic, "avg", timeGapStats.getAvg()));
            builder.append(String.format("ready_queue_time_gap{topic=\"%s\", type=\"%s\"} %d\n", topic, "max", timeGapStats.getMax()));
        }

        List<CamelliaDelayQueueTopicInfo> topicInfoList = server.getTopicInfoList();
        builder.append("# HELP topic_info Delay Queue Topic Info\n");
        builder.append("# TYPE topic_info gauge\n");
        for (CamelliaDelayQueueTopicInfo topicInfo : topicInfoList) {
            String topic = topicInfo.getTopic();
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "readyQueueSize", topicInfo.getReadyQueueSize()));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "ackQueueSize", topicInfo.getAckQueueSize()));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "waitingQueueSize", topicInfo.getWaitingQueueSize()));
            CamelliaDelayQueueTopicInfo.WaitingQueueInfo waitingQueueInfo = topicInfo.getWaitingQueueInfo();
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_0_1min", waitingQueueInfo.getSizeOf0To1min()));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_1min_10min", waitingQueueInfo.getSizeOf1minTo10min()));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_10min_30min", waitingQueueInfo.getSizeOf10minTo30min()));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_30min_1hour", waitingQueueInfo.getSizeOf30minTo1hour()));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_1hour_6hour", waitingQueueInfo.getSizeOf1hourTo6hour()));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_6hour_1day", waitingQueueInfo.getSizeOf6hourTo1day()));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_1day_7day", waitingQueueInfo.getSizeOf1dayTo7day()));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_7day_30day", waitingQueueInfo.getSizeOf7dayTo30day()));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_30day_infinite", waitingQueueInfo.getSizeOf30dayToInfinite()));
        }
        return builder.toString();
    }
}
