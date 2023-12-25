package com.netease.nim.camellia.metrics.sample;

import com.alibaba.fastjson.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2023/12/25
 */
@RestController
public class HotKeyMetricsMockController {

    private static final List<GarbageCollectorMXBean> garbageCollectorMXBeanList = ManagementFactory.getGarbageCollectorMXBeans();

    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

    private static final long startTime = System.currentTimeMillis() - 3*24*3600*1000L - 2*3600*1000L - 200*1000L;

    @GetMapping(value = "/hot_key/metrics", produces = "text/plain;charset=UTF-8")
    public String metrics() {
        StringBuilder builder = new StringBuilder();

        //info
        builder.append("# HELP info Hot Key Server Info\n");
        builder.append("# TYPE info gauge\n");
        builder.append("info");
        builder.append("{");
        builder.append("camellia_version=\"").append("1.2.22").append("\"").append(",");
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
        builder.append("# HELP cpu Hot Key Server Cpu\n");
        builder.append("# TYPE cpu gauge\n");
        builder.append(String.format("cpu{type=\"cpu_num\"} %d\n", 12));
        builder.append(String.format("cpu{type=\"usage\"} %d\n", ThreadLocalRandom.current().nextInt(50) + 200));

        //gc
        builder.append("# HELP gc Hot Key Server gc\n");
        builder.append("# TYPE gc gauge\n");
        for (GarbageCollectorMXBean bean : garbageCollectorMXBeanList) {
            builder.append(String.format("gc{name=\"%s\", type=\"count\"} %d\n", bean.getName(), bean.getCollectionCount()));
            builder.append(String.format("gc{name=\"%s\", type=\"time\"} %d\n", bean.getName(), bean.getCollectionTime()));
        }

        //thread
        builder.append("# HELP thread Hot Key Server Thread\n");
        builder.append("# TYPE thread gauge\n");
        builder.append(String.format("thread %d\n", 12));

        //client_connect
        builder.append("# HELP client_connect Hot Key Server Client Connect\n");
        builder.append("# TYPE client_connect gauge\n");
        builder.append(String.format("client_connect %d\n", ThreadLocalRandom.current().nextInt(10) + 50));

        //queue
        builder.append("# HELP queue Hot Key Server Queue\n");
        builder.append("# TYPE queue gauge\n");
        for (int i=0; i<12; i++) {
            boolean zero1 = ThreadLocalRandom.current().nextInt(100) < 95;
            if (zero1) {
                builder.append(String.format("queue{name=\"%s\", type=\"pending\"} %d\n", i, 0));
            } else {
                builder.append(String.format("queue{name=\"%s\", type=\"pending\"} %d\n", i, ThreadLocalRandom.current().nextInt(100)));
            }
            boolean zero2 = ThreadLocalRandom.current().nextInt(10) < 9;
            if (zero2) {
                builder.append(String.format("queue{name=\"%s\", type=\"discard\"} %d\n", i, 0));
            } else {
                builder.append(String.format("queue{name=\"%s\", type=\"discard\"} %d\n", i, ThreadLocalRandom.current().nextInt(10)));
            }
        }

        //qps
        builder.append("# HELP qps Hot Key Server QPS\n");
        builder.append("# TYPE qps gauge\n");
        builder.append(String.format("qps %d\n", ThreadLocalRandom.current().nextInt(1000) + 500));

        //qps_detail
        builder.append("# HELP qps_detail Hot Key Server QPS Detail\n");
        builder.append("# TYPE qps_detail gauge\n");
        builder.append(String.format("qps_detail{namespace=\"%s\", type=\"%s\"} %d\n",
                "namespace1", "QUERY", ThreadLocalRandom.current().nextInt(100)));
        builder.append(String.format("qps_detail{namespace=\"%s\", type=\"%s\"} %d\n",
                "namespace2", "QUERY", ThreadLocalRandom.current().nextInt(100)));

        //hot_key
        builder.append("# HELP hot_key Hot Key Server Hot Key\n");
        builder.append("# TYPE hot_key gauge\n");

        JSONObject json = new JSONObject();
        json.put("name", "rule1");
        json.put("type", "prefix_match");
        json.put("keyConfig", "key1");
        json.put("checkMillis", 1000);
        json.put("checkThreshold", 500);
        String rule = JSONObject.toJSONString(json).replaceAll("\"", "'");
        Set<String> sourceSet = new HashSet<>();
        sourceSet.add("app1");
        sourceSet.add("app2");
        StringBuilder source = new StringBuilder();
        for (String str : sourceSet) {
            source.append(str).append("|");
        }
        if (source.length() > 0) {
            source.deleteCharAt(source.length() - 1);
        }
        String key = "key1" + ThreadLocalRandom.current().nextInt(10);
        builder.append(String.format("hot_key{namespace=\"%s\", key=\"%s\", action=\"%s\", rule=\"%s\", source=\"%s\"} %d\n",
                "namespace1", key, "QUERY", rule, source, ThreadLocalRandom.current().nextInt(100) + 1000));
        return builder.toString();
    }
}
