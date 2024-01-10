package com.netease.nim.camellia.metrics.sample;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2023/12/25
 */
@RestController
public class DelayQueueMetricsMockController {

    private static final List<GarbageCollectorMXBean> garbageCollectorMXBeanList = ManagementFactory.getGarbageCollectorMXBeans();

    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

    private static final long startTime = System.currentTimeMillis() - 3*24*3600*1000L - 2*3600*1000L - 200*1000L;

    @GetMapping(value = "/delay_queue/metrics", produces = "text/plain;charset=UTF-8")
    public String metrics() {
        StringBuilder builder = new StringBuilder();

        //info
        builder.append("# HELP info Delay Queue Info\n");
        builder.append("# TYPE info gauge\n");
        builder.append("info");
        builder.append("{");
        builder.append("camellia_version=\"").append("1.2.24").append("\"").append(",");
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
        long freeMemory = (long) (1024*1024*1024*2.0f*(ThreadLocalRandom.current().nextDouble(0.1f) + 0.8f));
        long totalMemory = (long) (1024*1024*1024*5.0f);
        long maxMemory = (long) (1024*1024*1024*5.0f);
        long heapMemoryUsage = (long) (1024*1024*1024*3.0f*(ThreadLocalRandom.current().nextDouble(0.3f) + 0.6f));
        long noneHeapMemoryUsage = (long) (1024*1024*128*3.0f*(ThreadLocalRandom.current().nextDouble(0.3f) + 0.6f));
        long nettyDirectMemory = (long) (1024*1024*512*3.0f*(ThreadLocalRandom.current().nextDouble(0.3f) + 0.6f));
        builder.append(String.format("memory_info{type=\"free_memory\"} %d\n", freeMemory));
        builder.append(String.format("memory_info{type=\"total_memory\"} %d\n", totalMemory));
        builder.append(String.format("memory_info{type=\"max_memory\"} %d\n", maxMemory));
        builder.append(String.format("memory_info{type=\"heap_memory_usage\"} %d\n", heapMemoryUsage));
        builder.append(String.format("memory_info{type=\"no_heap_memory_usage\"} %d\n", noneHeapMemoryUsage));
        builder.append(String.format("memory_info{type=\"netty_direct_memory\"} %d\n", nettyDirectMemory));

        //cpu
        builder.append("# HELP cpu Delay Queue Cpu\n");
        builder.append("# TYPE cpu gauge\n");
        builder.append(String.format("cpu{type=\"cpu_num\"} %d\n", 12));
        builder.append(String.format("cpu{type=\"usage\"} %d\n", ThreadLocalRandom.current().nextInt(50) + 200));

        //gc
        builder.append("# HELP gc Delay Queue gc\n");
        builder.append("# TYPE gc gauge\n");
        for (GarbageCollectorMXBean bean : garbageCollectorMXBeanList) {
            builder.append(String.format("gc{name=\"%s\", type=\"count\"} %d\n", bean.getName(), bean.getCollectionCount()));
            builder.append(String.format("gc{name=\"%s\", type=\"time\"} %d\n", bean.getName(), bean.getCollectionTime()));
        }

        //request
        builder.append("# HELP request Delay Queue Request\n");
        builder.append("# TYPE request gauge\n");
        List<String> topics = new ArrayList<>();
        topics.add("queue1");
        topics.add("queue2");
        for (String topic : topics) {
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "sendMsg", ThreadLocalRandom.current().nextInt(100) + 10));
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "pullMsg", ThreadLocalRandom.current().nextInt(100) + 10));
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "deleteMsg", ThreadLocalRandom.current().nextInt(100) + 10));
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "ackMsg", ThreadLocalRandom.current().nextInt(100) + 10));
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "getMsg", ThreadLocalRandom.current().nextInt(100) + 10));
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "triggerMsgReady", ThreadLocalRandom.current().nextInt(100) + 10));
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "triggerMsgTimeout", ThreadLocalRandom.current().nextInt(100) + 10));
            builder.append(String.format("request{topic=\"%s\", type=\"%s\"} %d\n", topic, "triggerMsgEndLife", ThreadLocalRandom.current().nextInt(100) + 10));
        }

        //pull_msg_time_gap
        builder.append("# HELP pull_msg_time_gap Delay Queue Pull Msg Time Gap\n");
        builder.append("# TYPE pull_msg_time_gap gauge\n");
        for (String topic : topics) {
            builder.append(String.format("pull_msg_time_gap{topic=\"%s\", type=\"%s\"} %d\n", topic, "count", ThreadLocalRandom.current().nextInt(100) + 10));
            builder.append(String.format("pull_msg_time_gap{topic=\"%s\", type=\"%s\"} %f\n", topic, "avg", ThreadLocalRandom.current().nextDouble(50)+10));
            builder.append(String.format("pull_msg_time_gap{topic=\"%s\", type=\"%s\"} %d\n", topic, "max", ThreadLocalRandom.current().nextInt(1000) + 200));
        }

        //ready_queue_time_gap
        builder.append("# HELP ready_queue_time_gap Delay Queue Ready Queue Time Gap\n");
        builder.append("# TYPE ready_queue_time_gap gauge\n");
        for (String topic : topics) {
            builder.append(String.format("ready_queue_time_gap{topic=\"%s\", type=\"%s\"} %d\n", topic, "count", ThreadLocalRandom.current().nextInt(100) + 10));
            builder.append(String.format("ready_queue_time_gap{topic=\"%s\", type=\"%s\"} %f\n", topic, "avg", ThreadLocalRandom.current().nextDouble(50)+10));
            builder.append(String.format("ready_queue_time_gap{topic=\"%s\", type=\"%s\"} %d\n", topic, "max", ThreadLocalRandom.current().nextInt(1000) + 200));
        }

        builder.append("# HELP topic_info Delay Queue Topic Info\n");
        builder.append("# TYPE topic_info gauge\n");
        for (String topic : topics) {
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "readyQueueSize", ThreadLocalRandom.current().nextInt(10)));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "ackQueueSize", ThreadLocalRandom.current().nextInt(10)));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "waitingQueueSize", ThreadLocalRandom.current().nextInt(10)));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_0_1min", ThreadLocalRandom.current().nextInt(10)));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_1min_10min", ThreadLocalRandom.current().nextInt(10)));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_10min_30min", ThreadLocalRandom.current().nextInt(10)));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_30min_1hour", ThreadLocalRandom.current().nextInt(10)));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_1hour_6hour", ThreadLocalRandom.current().nextInt(10)));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_6hour_1day", ThreadLocalRandom.current().nextInt(10)));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_1day_7day", ThreadLocalRandom.current().nextInt(10)));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_7day_30day", ThreadLocalRandom.current().nextInt(10)));
            builder.append(String.format("topic_info{topic=\"%s\", type=\"%s\"} %d\n", topic, "size_30day_infinite", ThreadLocalRandom.current().nextInt(10)));
        }

        return builder.toString();
    }
}
