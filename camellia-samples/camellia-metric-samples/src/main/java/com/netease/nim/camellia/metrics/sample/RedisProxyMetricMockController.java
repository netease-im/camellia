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
public class RedisProxyMetricMockController {

    private static final List<GarbageCollectorMXBean> garbageCollectorMXBeanList = ManagementFactory.getGarbageCollectorMXBeans();

    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

    private static final long startTime = System.currentTimeMillis() - 3*24*3600*1000L - 2*3600*1000L - 200*1000L;

    @GetMapping(value = "/redis_proxy/metrics", produces = "text/plain;charset=UTF-8")
    public String metrics() {
        StringBuilder builder = new StringBuilder();

        builder.append("# HELP proxy_info Redis Proxy Info\n");
        builder.append("# TYPE proxy_info gauge\n");
        builder.append("proxy_info{");
        builder.append("proxy_version=").append("\"1.2.29\"").append(",");
        builder.append("arch=\"").append(osBean.getArch()).append("\"").append(",");
        builder.append("os_name=\"").append(osBean.getName()).append("\"").append(",");
        builder.append("os_version=\"").append(osBean.getVersion()).append("\"").append(",");
        builder.append("system_load_average=\"").append(osBean.getSystemLoadAverage()).append("\"").append(",");

        builder.append("vm_vendor=\"").append(runtimeMXBean.getVmVendor()).append("\"").append(",");
        builder.append("vm_name=\"").append(runtimeMXBean.getVmName()).append("\"").append(",");
        builder.append("vm_version=\"").append(runtimeMXBean.getVmVersion()).append("\"").append(",");
        builder.append("jvm_info=\"").append(System.getProperties().get("java.vm.info")).append("\"").append(",");
        builder.append("java_version=\"").append(System.getProperties().get("java.version")).append("\"").append(",");
        builder.append("proxy_mode=\"").append("standalone").append("\"").append(",");

        builder.append("}").append(" 1").append("\n");

        builder.append("# HELP uptime Redis Proxy Uptime\n");
        builder.append("# TYPE uptime gauge\n");
        builder.append(String.format("uptime %d\n", System.currentTimeMillis() - startTime));

        builder.append("# HELP start_time Redis Proxy Uptime\n");
        builder.append("# TYPE start_time gauge\n");
        builder.append(String.format("start_time %d\n", startTime));


        builder.append("# HELP memory_info Redis Proxy Memory\n");
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

        builder.append("# HELP cpu Redis Proxy Cpu\n");
        builder.append("# TYPE cpu gauge\n");
        builder.append(String.format("cpu{type=\"cpu_num\"} %d\n", 12));
        builder.append(String.format("cpu{type=\"usage\"} %d\n", ThreadLocalRandom.current().nextInt(50) + 200));

        builder.append("# HELP thread Redis Proxy Thread\n");
        builder.append("# TYPE thread gauge\n");
        builder.append(String.format("thread{type=\"boss_thread\"} %d\n", 1));
        builder.append(String.format("thread{type=\"work_thread\"} %d\n", 12));

        builder.append("# HELP gc Redis Proxy gc\n");
        builder.append("# TYPE gc gauge\n");
        for (GarbageCollectorMXBean bean : garbageCollectorMXBeanList) {
            builder.append(String.format("gc{name=\"%s\", type=\"count\"} %d\n", bean.getName(),
                    bean.getCollectionCount() + ThreadLocalRandom.current().nextInt(3)));
            builder.append(String.format("gc{name=\"%s\", type=\"time\"} %d\n", bean.getName(),
                    bean.getCollectionTime() + ThreadLocalRandom.current().nextInt(3)));
        }

        builder.append("# HELP client_connect Redis Proxy Connect Count\n");
        builder.append("# TYPE client_connect gauge\n");
        builder.append("client_connect ").append(ThreadLocalRandom.current().nextInt(100) + 10).append("\n");

        builder.append("# HELP client_connect_detail Redis Proxy Connect Count Detail\n");
        builder.append("# TYPE client_connect_detail gauge\n");
        builder.append(String.format("client_connect_detail{tenant=\"%s\"} %d\n", "1_default", ThreadLocalRandom.current().nextInt(50) + 5));
        builder.append(String.format("client_connect_detail{tenant=\"%s\"} %d\n", "2_default", ThreadLocalRandom.current().nextInt(50) + 5));

        builder.append("# HELP QPS Redis Proxy QPS\n");
        builder.append("# TYPE qps gauge\n");
        String totalFormat = "qps{type=\"%s\"} %d\n";
        long maxReadQps = ThreadLocalRandom.current().nextInt(50) + 300;
        long maxWriteQps = ThreadLocalRandom.current().nextInt(30) + 100;
        long maxQps = maxReadQps + maxWriteQps;

        long readQps = maxReadQps - ThreadLocalRandom.current().nextInt(50);
        long writeQps = maxWriteQps - ThreadLocalRandom.current().nextInt(30);
        long qps = readQps + writeQps;
        builder.append(String.format(totalFormat, "qps", qps));
        builder.append(String.format(totalFormat, "write_qps", writeQps));
        builder.append(String.format(totalFormat, "read_qps", readQps));
        builder.append(String.format(totalFormat, "max_qps", maxQps));
        builder.append(String.format(totalFormat, "max_write_qps", maxWriteQps));
        builder.append(String.format(totalFormat, "max_read_qps", maxReadQps));

        builder.append("# HELP command_qps Redis Proxy Total Command QPS\n");
        builder.append("# TYPE command_qps gauge\n");
        builder.append(String.format("command_qps{command=\"%s\"} %d\n", "get", ThreadLocalRandom.current().nextInt(300) + 10));
        builder.append(String.format("command_qps{command=\"%s\"} %d\n", "set", ThreadLocalRandom.current().nextInt(200) + 10));
        builder.append(String.format("command_qps{command=\"%s\"} %d\n", "expire", ThreadLocalRandom.current().nextInt(100) + 10));
        builder.append(String.format("command_qps{command=\"%s\"} %d\n", "zadd", ThreadLocalRandom.current().nextInt(50) + 10));
        builder.append(String.format("command_qps{command=\"%s\"} %d\n", "lpush", ThreadLocalRandom.current().nextInt(50) + 10));
        builder.append(String.format("command_qps{command=\"%s\"} %d\n", "zrange", ThreadLocalRandom.current().nextInt(100) + 10));
        builder.append(String.format("command_qps{command=\"%s\"} %d\n", "hset", ThreadLocalRandom.current().nextInt(100) + 10));
        builder.append(String.format("command_qps{command=\"%s\"} %d\n", "hgetall", ThreadLocalRandom.current().nextInt(70) + 10));

        builder.append("# HELP command_spend_stats Redis Proxy Spend Stats\n");
        builder.append("# TYPE command_spend_stats gauge\n");
        List<String> commands = new ArrayList<>();
        commands.add("get");
        commands.add("set");
        commands.add("expire");
        commands.add("zadd");
        for (String command : commands) {
            int count = ThreadLocalRandom.current().nextInt(100);
            double avg = ThreadLocalRandom.current().nextDouble(0.3f);
            double p50 = ThreadLocalRandom.current().nextDouble(0.3f);
            double p90 = p50 + ThreadLocalRandom.current().nextDouble(0.1f);
            double p99 = p90 + ThreadLocalRandom.current().nextDouble(0.3f);
            double max = p99 + ThreadLocalRandom.current().nextDouble(10.0f);
            builder.append(String.format("command_spend_stats{command=\"%s\",type=\"count\"} %d\n", command, count));
            builder.append(String.format("command_spend_stats{command=\"%s\",type=\"avg\"} %f\n", command, avg));
            builder.append(String.format("command_spend_stats{command=\"%s\",type=\"max\"} %f\n", command, max));
            builder.append(String.format("command_spend_stats{command=\"%s\",type=\"p50\"} %f\n", command, p50));
            builder.append(String.format("command_spend_stats{command=\"%s\",type=\"p90\"} %f\n", command, p90));
            builder.append(String.format("command_spend_stats{command=\"%s\",type=\"p99\"} %f\n", command, p99));
        }

        builder.append("# HELP tenant_qps Redis Proxy Tenant QPS\n");
        builder.append("# TYPE tenant_qps gauge\n");
        builder.append(String.format("tenant_qps{tenant=\"%s\"} %d\n", "1_default", ThreadLocalRandom.current().nextInt(200) + 10));
        builder.append(String.format("tenant_qps{tenant=\"%s\"} %d\n", "2_default", ThreadLocalRandom.current().nextInt(300) + 50));


        builder.append("# HELP tenant_command_qps Redis Proxy Tenant Command QPS\n");
        builder.append("# TYPE tenant_command_qps gauge\n");
        builder.append(String.format("tenant_command_qps{tenant=\"%s\",command=\"%s\"} %d\n", "1_default", "get", ThreadLocalRandom.current().nextInt(300) + 10));
        builder.append(String.format("tenant_command_qps{tenant=\"%s\",command=\"%s\"} %d\n", "1_default", "set", ThreadLocalRandom.current().nextInt(200) + 10));
        builder.append(String.format("tenant_command_qps{tenant=\"%s\",command=\"%s\"} %d\n", "2_default", "get", ThreadLocalRandom.current().nextInt(250) + 30));
        builder.append(String.format("tenant_command_qps{tenant=\"%s\",command=\"%s\"} %d\n", "2_default", "expire", ThreadLocalRandom.current().nextInt(50) + 30));

        builder.append("# HELP tenant_command_spend_stats Redis Proxy Spend Stats\n");
        builder.append("# TYPE tenant_command_spend_stats gauge\n");
        List<String> tenantCommands = new ArrayList<>();
        tenantCommands.add("get");
        tenantCommands.add("set");
        tenantCommands.add("expire");
        List<String> tenants = new ArrayList<>();
        tenants.add("1_default");
        tenants.add("2_default");
        for (String tenant : tenants) {
            for (String command : tenantCommands) {
                int count = ThreadLocalRandom.current().nextInt(100);
                double avg = ThreadLocalRandom.current().nextDouble(0.3f);
                double p50 = ThreadLocalRandom.current().nextDouble(0.3f);
                double p90 = p50 + ThreadLocalRandom.current().nextDouble(0.1f);
                double p99 = p90 + ThreadLocalRandom.current().nextDouble(0.3f);
                double max = p99 + ThreadLocalRandom.current().nextDouble(10.0f);
                builder.append(String.format("tenant_command_spend_stats{tenant=\"%s\",command=\"%s\",type=\"count\"} %d\n", tenant, command, count));
                builder.append(String.format("tenant_command_spend_stats{tenant=\"%s\",command=\"%s\",type=\"avg\"} %f\n", tenant, command, avg));
                builder.append(String.format("tenant_command_spend_stats{tenant=\"%s\",command=\"%s\",type=\"max\"} %f\n", tenant, command, max));
                builder.append(String.format("tenant_command_spend_stats{tenant=\"%s\",command=\"%s\",type=\"p50\"} %f\n", tenant, command, p50));
                builder.append(String.format("tenant_command_spend_stats{tenant=\"%s\",command=\"%s\",type=\"p90\"} %f\n", tenant,  command, p90));
                builder.append(String.format("tenant_command_spend_stats{tenant=\"%s\",command=\"%s\",type=\"p99\"} %f\n", tenant, command, p99));
            }
        }

        builder.append("# HELP upstream_redis_connect Redis Proxy Upstream Redis Connect Count\n");
        builder.append("# TYPE upstream_redis_connect gauge\n");
        builder.append("upstream_redis_connect ").append(ThreadLocalRandom.current().nextInt(150) + 30).append("\n");

        builder.append("# HELP upstream_redis_connect_detail Redis Proxy Upstream Redis Connect Count Detail\n");
        builder.append("# TYPE upstream_redis_connect_detail gauge\n");
        builder.append(String.format("upstream_redis_connect_detail{upstream=\"%s\"} %d\n", "***@10.2.2.1:6379", ThreadLocalRandom.current().nextInt(2) + 10));
        builder.append(String.format("upstream_redis_connect_detail{upstream=\"%s\"} %d\n", "***@10.2.2.2:6379", ThreadLocalRandom.current().nextInt(2) + 10));
        builder.append(String.format("upstream_redis_connect_detail{upstream=\"%s\"} %d\n", "***@10.2.2.3:6379", ThreadLocalRandom.current().nextInt(2) + 10));

        builder.append("# HELP upstream_redis_spend_stats Redis Proxy Upstream Redis Spend Stats\n");
        builder.append("# TYPE upstream_redis_spend_stats gauge\n");

        List<String> upstreams = new ArrayList<>();
        upstreams.add("***@10.2.2.1:6379");
        upstreams.add("***@10.2.2.2:6379");
        upstreams.add("***@10.2.2.3:6379");
        for (String upstream : upstreams) {
            int count = ThreadLocalRandom.current().nextInt(300) + 50;
            double avg = ThreadLocalRandom.current().nextDouble(0.3f);
            double p50 = ThreadLocalRandom.current().nextDouble(0.3f);
            double p90 = p50 + ThreadLocalRandom.current().nextDouble(0.1f);
            double p99 = p90 + ThreadLocalRandom.current().nextDouble(0.3f);
            double max = p99 + ThreadLocalRandom.current().nextDouble(10.0f);
            builder.append(String.format("upstream_redis_spend_stats{upstream=\"%s\", type=\"count\"} %d\n", upstream, count));
            builder.append(String.format("upstream_redis_spend_stats{upstream=\"%s\", type=\"avg\"} %f\n", upstream, avg));
            builder.append(String.format("upstream_redis_spend_stats{upstream=\"%s\", type=\"max\"} %f\n", upstream, max));
            builder.append(String.format("upstream_redis_spend_stats{upstream=\"%s\", type=\"p50\"} %f\n", upstream, p50));
            builder.append(String.format("upstream_redis_spend_stats{upstream=\"%s\", type=\"p90\"} %f\n", upstream, p90));
            builder.append(String.format("upstream_redis_spend_stats{upstream=\"%s\", type=\"p99\"} %f\n", upstream, p99));
        }


        builder.append("# HELP proxy_route_conf Redis Proxy Upstream Redis Route Conf\n");
        builder.append("# TYPE proxy_route_conf gauge\n");
        builder.append(String.format("proxy_route_conf{tenant=\"%s\", route=\"%s\"} 1\n", "1_default", "redis://@127.0.0.1:6379"));
        builder.append(String.format("proxy_route_conf{tenant=\"%s\", route=\"%s\"} 1\n", "2_default", "redis-cluster://@127.0.0.1:6379,127.0.0.2:6379"));
        builder.append(String.format("proxy_route_conf{tenant=\"%s\", route=\"%s\"} 1\n", "3_default", "{'type':'simple','operation':{'read':'redis://passwd123@127.0.0.1:6379','type':'rw_separate','write':'redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master'}}"));


        builder.append("# HELP upstream_redis_qps Redis Proxy Upstream Upstream Redis QPS\n");
        builder.append("# TYPE upstream_redis_qps gauge\n");
        builder.append(String.format("upstream_redis_qps{upstream=\"%s\"} %d\n", "redis://@127.0.0.1:6379", ThreadLocalRandom.current().nextInt(50) + 200));
        builder.append(String.format("upstream_redis_qps{upstream=\"%s\"} %d\n", "redis-cluster://@127.0.0.1:6379,127.0.0.2:6379", ThreadLocalRandom.current().nextInt(70) + 300));


        builder.append("# HELP client_fail Redis Proxy Fail\n");
        builder.append("# TYPE client_fail gauge\n");
        builder.append(String.format("client_fail{reason=\"%s\",} %d\n", "ChannelNotActive", ThreadLocalRandom.current().nextInt(10)));
        builder.append(String.format("client_fail{reason=\"%s\",} %d\n", "ERR param wrong", ThreadLocalRandom.current().nextInt(3)));


        builder.append("# HELP upstream_fail Redis Proxy Upstream Fail\n");
        builder.append("# TYPE upstream_fail gauge\n");
        builder.append(String.format("upstream_fail{upstream=\"%s\",command=\"%s\",msg=\"%s\"} %d\n", "redis://@127.0.0.1:6379", "get", "ERR param wrong", ThreadLocalRandom.current().nextInt(3)));
        builder.append(String.format("upstream_fail{upstream=\"%s\",command=\"%s\",msg=\"%s\"} %d\n", "redis://@127.0.0.1:6379", "set", "ERR internal error", ThreadLocalRandom.current().nextInt(3)));
        builder.append(String.format("upstream_fail{upstream=\"%s\",command=\"%s\",msg=\"%s\"} %d\n", "redis-cluster://@127.0.0.1:6379,127.0.0.2:6379", "get", "ERR param wrong", ThreadLocalRandom.current().nextInt(3)));


        builder.append("# HELP slow_command Redis Proxy Slow Command\n");
        builder.append("# TYPE slow_command gauge\n");
        for (int i=0; i<3; i++) {
            double spend1 = ThreadLocalRandom.current().nextDouble(2100);
            if (spend1 > 2000) {
                String key = "key" + ThreadLocalRandom.current().nextInt(10);
                builder.append(String.format("slow_command{tenant=\"%s\",command=\"%s\",keys=\"%s\"} %f\n", "1_default", "get", key, spend1));
            }
        }

        builder.append("# HELP string_big_key Redis Proxy Big Key\n");
        builder.append("# TYPE string_big_key gauge\n");
        for (int i=0; i<3; i++) {
            long size = ThreadLocalRandom.current().nextInt(1024*1024+1024*30);
            if (size > 1024*1024) {
                String key = "key" + ThreadLocalRandom.current().nextInt(10);
                builder.append(String.format("string_big_key{tenant=\"%s\",commandType=\"%s\",command=\"%s\",key=\"%s\"} %d\n", "1_default", "string", "get", key, size));
            }
        }
        builder.append("# HELP collection_big_key Redis Proxy Big Key\n");
        builder.append("# TYPE collection_big_key gauge\n");
        for (int i=0; i<3; i++) {
            long size = ThreadLocalRandom.current().nextInt(10000+3000);
            if (size > 10000) {
                String key = "zset_key" + ThreadLocalRandom.current().nextInt(10);
                builder.append(String.format("collection_big_key{tenant=\"%s\",commandType=\"%s\",command=\"%s\",key=\"%s\"} %d\n", "2_default", "zset", "zrange", key, size));
            }
        }


        builder.append("# HELP hot_key Redis Proxy Hot Key\n");
        builder.append("# TYPE hot_key gauge\n");
        for (int i=0; i<3; i++) {
            int size = ThreadLocalRandom.current().nextInt(1500);
            if (size > 1000) {
                String key = "hkey" + ThreadLocalRandom.current().nextInt(10);
                builder.append(String.format("hot_key{tenant=\"%s\",key=\"%s\",type=\"qps\"} %d\n", "1_default", key, size));
                builder.append(String.format("hot_key{tenant=\"%s\",key=\"%s\",type=\"count\"} %d\n", "1_default", key, size * (ThreadLocalRandom.current().nextInt(3) + 1)));
            }
        }

        builder.append("# HELP hot_key_cache_hit Redis Proxy Hot Key Cache Hit\n");
        builder.append("# TYPE hot_key_cache_hit gauge\n");
        for (int i=0; i<3; i++) {
            int size = ThreadLocalRandom.current().nextInt(1500);
            if (size > 1000) {
                String key = "hkeyhit" + ThreadLocalRandom.current().nextInt(10);
                builder.append(String.format("hot_key_cache_hit{tenant=\"%s\",key=\"%s\"} %d\n", "1_default", key, size));
            }
        }

        System.out.println("here");
        return builder.toString();
    }
}
