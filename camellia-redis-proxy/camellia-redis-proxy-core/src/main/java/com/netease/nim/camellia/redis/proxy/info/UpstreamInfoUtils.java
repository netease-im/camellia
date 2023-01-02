package com.netease.nim.camellia.redis.proxy.info;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceUtil;
import com.netease.nim.camellia.redis.proxy.upstream.sentinel.RedisSentinelUtils;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.AsyncCamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.AsyncCamelliaRedisTemplateChooser;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClient;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientAddr;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientHub;
import com.netease.nim.camellia.redis.proxy.upstream.utils.HostAndPort;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.redis.resource.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2021/6/30
 */
public class UpstreamInfoUtils {

    private static final Logger logger = LoggerFactory.getLogger(UpstreamInfoUtils.class);

    public static JSONObject monitorJson(String url) {
        boolean pass = RedisResourceUtil.RedisResourceTableChecker.check(ResourceTableUtil.simpleTable(new Resource(url)));
        if (!pass) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        JSONArray infoJsonArray = new JSONArray();
        JSONObject infoJson = new JSONObject();
        infoJsonArray.add(infoJson);
        JSONArray otherInfoJsonArray = new JSONArray();
        infoJson.put("url", PasswordMaskUtils.maskResource(url));
        JSONArray nodeJsonArray = new JSONArray();
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource(url));
        if (resource instanceof RedisClusterResource) {
            infoJson.put("type", "cluster");
            String redisClusterInfo = getRedisClusterInfo((RedisClusterResource) resource);
            infoJson.put("status", "unknown");
            if (redisClusterInfo != null) {
                Map<String, String> map = parseKv(redisClusterInfo);
                if (map != null) {
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        otherInfoJsonArray.add(infoItem(entry.getKey(), entry.getValue()));
                    }
                    if (map.containsKey("cluster_state")) {
                        infoJson.put("status", map.get("cluster_state"));
                    }
                }
                long totalMaxMemory = 0;
                long totalUsedMemory = 0;
                long totalQps = 0;
                long totalKeyCount = 0;
                long totalExpireKeyCount = 0;
                List<ClusterNodeInfo> redisClusterNodeInfo = getRedisClusterNodeInfo((RedisClusterResource) resource);
                if (redisClusterNodeInfo != null) {
                    boolean safety = redisClusterSafety(redisClusterNodeInfo);
                    otherInfoJsonArray.add(infoItem("cluster_safety", safety ? "yes" : "no"));
                    for (ClusterNodeInfo clusterNodeInfo : redisClusterNodeInfo) {
                        String master = clusterNodeInfo.master;
                        String[] split = master.split("@");
                        String[] split1 = split[0].split(":");
                        String host = split1[0];
                        int port = Integer.parseInt(split1[1]);
                        RedisClientAddr addr = new RedisClientAddr(host, port, ((RedisClusterResource) resource).getUserName(), ((RedisClusterResource) resource).getPassword());
                        RedisInfo redisInfo = getRedisInfo(addr);
                        if (redisInfo != null) {
                            totalMaxMemory += redisInfo.maxMemory;
                            totalUsedMemory += redisInfo.usedMemory;
                            totalQps += redisInfo.qps;
                            totalKeyCount += redisInfo.keyCount;
                            totalExpireKeyCount += redisInfo.expireKeyCount;
                            JSONObject nodeJson = toNodeJson(addr, redisInfo);
                            nodeJsonArray.add(nodeJson);
                        }
                    }
                }
                infoJson(infoJson, totalMaxMemory, totalUsedMemory, totalQps, totalKeyCount, totalExpireKeyCount);
            }
        } else if (resource instanceof RedisSentinelResource) {
            infoJson.put("type", "sentinel");
            RedisSentinelInfo redisSentinelInfo = getRedisSentinelInfo(((RedisSentinelResource) resource).getNodes(),
                    ((RedisSentinelResource) resource).getUserName(), ((RedisSentinelResource) resource).getPassword(), ((RedisSentinelResource) resource).getMaster());
            if (redisSentinelInfo.master != null && redisSentinelInfo.redisInfo != null) {
                infoJson.put("status", "ok");
                otherInfoJsonArray.add(infoItem("master_node", PasswordMaskUtils.maskAddr(redisSentinelInfo.master)));
                JSONObject nodeJson = toNodeJson(redisSentinelInfo.master, redisSentinelInfo.redisInfo);
                nodeJsonArray.add(nodeJson);
                infoJson(infoJson, redisSentinelInfo.redisInfo.maxMemory, redisSentinelInfo.redisInfo.usedMemory,
                        redisSentinelInfo.redisInfo.qps, redisSentinelInfo.redisInfo.keyCount, redisSentinelInfo.redisInfo.expireKeyCount);
            } else {
                infoJson.put("status", "unknown");
            }
        } else if (resource instanceof RedisResource) {
            infoJson.put("type", "standalone");
            RedisClientAddr addr = new RedisClientAddr(((RedisResource) resource).getHost(),
                    ((RedisResource) resource).getPort(), ((RedisResource) resource).getUserName(), ((RedisResource) resource).getPassword());
            RedisInfo redisInfo = getRedisInfo(addr);
            if (redisInfo != null) {
                infoJson.put("status", "ok");
                infoJson(infoJson, redisInfo.maxMemory, redisInfo.usedMemory,
                        redisInfo.qps, redisInfo.keyCount, redisInfo.expireKeyCount);
                JSONObject nodeJson = toNodeJson(addr, redisInfo);
                nodeJsonArray.add(nodeJson);
            } else {
                infoJson.put("status", "unknown");
            }
        } else {
            infoJsonArray.add(infoItem("type", "unknown"));
        }
        jsonObject.put("info", infoJsonArray);
        jsonObject.put("nodeInfo", nodeJsonArray);
        jsonObject.put("otherInfo", otherInfoJsonArray);
        return jsonObject;
    }

    private static void infoJson(JSONObject infoJson, long maxMemory, long usedMemory, long qps, long keyCount, long expireKeyCount) {
        double memoryUsedRate = 0.0;
        if (maxMemory != 0) {
            memoryUsedRate = (double) usedMemory / maxMemory;
        }
        infoJson.put("maxmemory", maxMemory);
        infoJson.put("maxmemory_hum", ProxyInfoUtils.humanReadableByteCountBin(maxMemory));
        infoJson.put("used_memory", usedMemory);
        infoJson.put("used_memory_human", ProxyInfoUtils.humanReadableByteCountBin(usedMemory));
        infoJson.put("memory_used_rate", memoryUsedRate);
        infoJson.put("memory_used_rate_human", String.format("%.2f", memoryUsedRate * 100.0) + "%");
        infoJson.put("qps", qps);
        infoJson.put("key_count", keyCount);
        infoJson.put("expire_key_count", expireKeyCount);
    }

    private static JSONObject toNodeJson(RedisClientAddr clientAddr, RedisInfo redisInfo) {
        JSONObject json = new JSONObject();
        Map<String, String> map = parseKv(redisInfo.string);
        Map<String, Object> formatMap = new HashMap<>();
        HashSet<String> keys = new HashSet<>(map.keySet());
        for (String key : keys) {
            String value = map.get(key);
            try {
                double number = Double.parseDouble(value);
                formatMap.put(key, number);
                continue;
            } catch (NumberFormatException ignore) {
            }
            try {
                long number = Long.parseLong(value);
                formatMap.put(key, number);
                continue;
            } catch (NumberFormatException ignore) {
            }
            formatMap.put(key, value);
        }
        json.putAll(formatMap);
        json.put("node_url", PasswordMaskUtils.maskAddr(clientAddr.getUrl()));
        return json;
    }

    private static JSONObject infoItem(String key, Object value) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", key);
        jsonObject.put("value", value);
        return jsonObject;
    }

    public static String upstreamInfo(Long bid, String bgroup, AsyncCamelliaRedisTemplateChooser chooser, boolean parseJson) {
        try {
            AsyncCamelliaRedisTemplate template = chooser.choose(bid, bgroup);
            if (template == null) {
                return null;
            }

            JSONObject jsonObject = new JSONObject();

            ResourceTable resourceTable = template.getResourceTable();
            StringBuilder builder = new StringBuilder();
            JSONObject upstreamInfo = new JSONObject();
            jsonObject.put("Upstream-Info", upstreamInfo);

            builder.append("# Upstream-Info").append("\r\n");

            String routeConf = ReadableResourceTableUtil.readableResourceTable(PasswordMaskUtils.maskResourceTable(resourceTable));
            builder.append("route_conf:").append(routeConf).append("\r\n");

            try {
                upstreamInfo.put("route_conf", JSONObject.parseObject(routeConf));
            } catch (Exception e) {
                upstreamInfo.put("route_conf", routeConf);
            }

            if (bid != null && bgroup != null) {
                builder.append("bid:").append(bid).append("\r\n");
                builder.append("bgroup:").append(bgroup).append("\r\n");

                upstreamInfo.put("bid", String.valueOf(bid));
                upstreamInfo.put("bgroup", bgroup);
            }
            List<Resource> resources = new ArrayList<>(ResourceUtil.getAllResources(resourceTable));
            builder.append("upstream_cluster_count:").append(resources.size()).append("\r\n");
            upstreamInfo.put("upstream_cluster_count", String.valueOf(resources.size()));
            for (int i=0; i<resources.size(); i++) {
                Resource resource = resources.get(i);
                builder.append("upstream").append(i).append("_url:").append(resource.getUrl()).append("\r\n");
                upstreamInfo.put("upstream" + i + "_url", resource.getUrl());
            }
            builder.append("\r\n");
            for (int i=0; i<resources.size(); i++) {
                JSONObject upstream = new JSONObject();
                upstreamInfo.put("Upstream" + i, upstream);

                Resource resource = resources.get(i);
                builder.append("## Upstream").append(i).append("\r\n");
                builder.append("url:").append(PasswordMaskUtils.maskResource(resource.getUrl())).append("\r\n");

                upstream.put("url", PasswordMaskUtils.maskResource(resource.getUrl()));

                Resource redisResource = RedisResourceUtil.parseResourceByUrl(resource);
                if (redisResource instanceof RedisResource) {
                    JSONObject redisNodeInfoJson = new JSONObject();
                    upstream.put("redis-node-info", redisNodeInfoJson);

                    RedisClientAddr addr = new RedisClientAddr(((RedisResource) redisResource).getHost(),
                            ((RedisResource) redisResource).getPort(), ((RedisResource) redisResource).getUserName(), ((RedisResource) redisResource).getPassword());
                    builder.append("### redis-node-info").append("\r\n");
                    RedisInfo redisInfo = getRedisInfo(addr);
                    if (redisInfo != null) {
                        builder.append(redisInfo.string);

                        Map<String, String> map = parseKv(redisInfo.string);
                        if (map != null) {
                            redisNodeInfoJson.putAll(map);
                        }
                    }
                } else if (redisResource instanceof RedisSentinelResource || redisResource instanceof RedisSentinelSlavesResource) {
                    List<RedisSentinelResource.Node> sentinels;
                    String userName;
                    String password;
                    String master;
                    if (redisResource instanceof RedisSentinelResource) {
                        sentinels = ((RedisSentinelResource) redisResource).getNodes();
                        userName = ((RedisSentinelResource) redisResource).getUserName();
                        password = ((RedisSentinelResource) redisResource).getPassword();
                        master = ((RedisSentinelResource) redisResource).getMaster();
                    } else {
                        sentinels = ((RedisSentinelSlavesResource) redisResource).getNodes();
                        userName = ((RedisSentinelSlavesResource) redisResource).getUserName();
                        password = ((RedisSentinelSlavesResource) redisResource).getPassword();
                        master = ((RedisSentinelSlavesResource) redisResource).getMaster();
                    }
                    JSONObject redisNodeInfoJson = new JSONObject();
                    upstream.put("redis-node-info", redisNodeInfoJson);

                    builder.append("### redis-node-info").append("\r\n");
                    RedisSentinelInfo redisSentinelInfo = getRedisSentinelInfo(sentinels, userName, password, master);
                    if (redisSentinelInfo.master != null) {
                        builder.append("master_url:").append(redisSentinelInfo.master).append("\r\n");

                        redisNodeInfoJson.put("master_url", redisSentinelInfo.master.toString());
                    }
                    if (redisSentinelInfo.redisInfo != null && redisSentinelInfo.redisInfo.string != null) {
                        builder.append(redisSentinelInfo.redisInfo.string);
                        Map<String, String> map = parseKv(redisSentinelInfo.redisInfo.string);
                        if (map != null) {
                            redisNodeInfoJson.putAll(map);
                        }
                    }
                } else if (redisResource instanceof RedisClusterResource) {

                    JSONObject redisClusterInfoJson = new JSONObject();
                    upstream.put("redis-cluster-info", redisClusterInfoJson);

                    String redisClusterInfo = getRedisClusterInfo((RedisClusterResource) redisResource);
                    if (redisClusterInfo != null) {
                        builder.append("### redis-cluster-info").append("\r\n");
                        builder.append(redisClusterInfo);

                        Map<String, String> map = parseKv(redisClusterInfo);
                        if (map != null) {
                            redisClusterInfoJson.putAll(map);
                        }
                    }
                    long totalMaxMemory = 0;
                    long totalUsedMemory = 0;
                    StringBuilder clusterBuilder = new StringBuilder();
                    List<ClusterNodeInfo> redisClusterNodeInfo = getRedisClusterNodeInfo((RedisClusterResource) redisResource);
                    if (redisClusterNodeInfo != null) {
                        boolean safety = redisClusterSafety(redisClusterNodeInfo);
                        builder.append("cluster_safety:").append(safety ? "yes" : "no").append("\r\n");
                        redisClusterInfoJson.put("cluster_safety", safety ? "yes" : "no");

                        StringBuilder redisNodeInfoBuilder = new StringBuilder();
                        redisNodeInfoBuilder.append("### redis-node-info").append("\r\n");

                        JSONObject redisNodeInfoJson = new JSONObject();
                        upstream.put("redis-node-info", redisNodeInfoJson);

                        int k = 0;
                        Map<String, RedisInfo> map = new HashMap<>();
                        for (ClusterNodeInfo clusterNodeInfo : redisClusterNodeInfo) {
                            String master = clusterNodeInfo.master;
                            String[] split = master.split("@");
                            String[] split1 = split[0].split(":");
                            String host = split1[0];
                            int port = Integer.parseInt(split1[1]);
                            RedisInfo redisInfo = getRedisInfo(new RedisClientAddr(host, port, ((RedisClusterResource) redisResource).getUserName(), ((RedisClusterResource) redisResource).getPassword()));
                            redisNodeInfoBuilder.append("#### node").append(k).append("\r\n");

                            JSONObject node = new JSONObject();
                            redisNodeInfoJson.put("node" + k, node);

                            redisNodeInfoBuilder.append("master_url=").append(clusterNodeInfo.master).append("\r\n");
                            node.put("master_url", clusterNodeInfo.master);
                            if (redisInfo != null) {
                                redisNodeInfoBuilder.append(redisInfo.string);
                                totalMaxMemory += redisInfo.maxMemory;
                                totalUsedMemory += redisInfo.usedMemory;
                                map.put(master, redisInfo);

                                Map<String, String> map1 = parseKv(redisInfo.string);
                                if (map1 != null) {
                                    node.putAll(map1);
                                }
                            }
                            k ++;
                        }

                        StringBuilder redisClusterNodeInfoBuilder = new StringBuilder();
                        redisClusterNodeInfoBuilder.append("### redis-cluster-node-info").append("\r\n");

                        JSONObject redisClusterNodeInfoJson = new JSONObject();
                        upstream.put("redis-cluster-node-info", redisClusterNodeInfoJson);

                        int j = 0;
                        for (ClusterNodeInfo clusterNodeInfo : redisClusterNodeInfo) {

                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("master=").append(clusterNodeInfo.master)
                                    .append(",slave=").append(clusterNodeInfo.slaves)
                                    .append(",slots=").append(clusterNodeInfo.slots);

                            RedisInfo redisInfo = map.get(clusterNodeInfo.master);
                            if (redisInfo != null) {
                                stringBuilder.append(",maxmemory=").append(ProxyInfoUtils.humanReadableByteCountBin(redisInfo.maxMemory));
                                stringBuilder.append(",used_memory=").append(ProxyInfoUtils.humanReadableByteCountBin(redisInfo.usedMemory));
                                double rate = 0.0;
                                if (redisInfo.maxMemory != 0) {
                                    rate = (double) redisInfo.usedMemory / redisInfo.maxMemory;
                                }
                                stringBuilder.append(",memory_used_rate=").append(String.format("%.2f", rate * 100.0)).append("%");
                            }
                            redisClusterNodeInfoBuilder.append("node").append(j).append(":").append(stringBuilder);
                            redisClusterNodeInfoBuilder.append("\r\n");

                            redisClusterNodeInfoJson.put("node" + j, stringBuilder);
                            j++;
                        }
                        clusterBuilder.append(redisClusterNodeInfoBuilder).append(redisNodeInfoBuilder);
                    }
                    double clusterMemoryUsedRate = 0.0;
                    if (totalMaxMemory != 0) {
                        clusterMemoryUsedRate = (double) totalUsedMemory / totalMaxMemory;
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("cluster_maxmemory:").append(totalMaxMemory).append("\r\n");
                    stringBuilder.append("cluster_maxmemory_human:").append(ProxyInfoUtils.humanReadableByteCountBin(totalMaxMemory)).append("\r\n");
                    stringBuilder.append("cluster_used_memory:").append(totalUsedMemory).append("\r\n");
                    stringBuilder.append("cluster_used_memory_human:").append(ProxyInfoUtils.humanReadableByteCountBin(totalUsedMemory)).append("\r\n");
                    stringBuilder.append("cluster_memory_used_rate:").append(clusterMemoryUsedRate).append("\r\n");
                    stringBuilder.append("cluster_memory_used_rate_human:").append(String.format("%.2f", clusterMemoryUsedRate * 100.0)).append("%").append("\r\n");
                    Map<String, String> map = parseKv(stringBuilder.toString());
                    if (map != null) {
                        redisClusterInfoJson.putAll(map);
                    }
                    builder.append(stringBuilder).append(clusterBuilder);
                }
                builder.append("\r\n");
            }
            if (parseJson) {
                return jsonObject.toJSONString();
            }
            return builder.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }


    private static final List<String> redisInfoKeys = new ArrayList<>();
    static {
        redisInfoKeys.add("redis_version");
        redisInfoKeys.add("used_memory");
        redisInfoKeys.add("used_memory_human");
        redisInfoKeys.add("maxmemory");
        redisInfoKeys.add("maxmemory_human");
        redisInfoKeys.add("maxmemory_policy");
        redisInfoKeys.add("hz");
        redisInfoKeys.add("role");
        redisInfoKeys.add("connected_slaves");
        redisInfoKeys.add("db0");
        redisInfoKeys.add("instantaneous_ops_per_sec");
    }

    private static class RedisInfo {
        private String string;
        private long maxMemory;
        private long usedMemory;
        private long qps;
        private long keyCount;
        private long expireKeyCount;
        private long avgTtl;
    }
    private static RedisInfo getRedisInfo(RedisClientAddr addr) {
        Map<String, String> map = getInfoMap(addr.getHost(), addr.getPort(), addr.getUserName(), addr.getPassword(), new byte[][] {RedisCommand.INFO.raw()});
        if (map == null) {
            return null;
        } else {
            StringBuilder builder = new StringBuilder();
            RedisInfo redisInfo = new RedisInfo();
            for (String key : redisInfoKeys) {
                String value = map.get(key);
                if (key.equals("db0") && value != null) {
                    try {
                        String[] split = value.trim().split(",");
                        redisInfo.keyCount = Long.parseLong(split[0].split("=")[1]);
                        redisInfo.expireKeyCount = Long.parseLong(split[1].split("=")[1]);
                        redisInfo.avgTtl = Long.parseLong(split[2].split("=")[1]);
                        builder.append("key_count:").append(redisInfo.keyCount).append("\r\n");
                        builder.append("expire_key_count:").append(redisInfo.expireKeyCount).append("\r\n");
                        builder.append("avg_ttl:").append(redisInfo.avgTtl).append("\r\n");
                    } catch (Exception ignore) {
                    }
                    continue;
                }
                if (key.equals("instantaneous_ops_per_sec") && value != null) {
                    try {
                        redisInfo.qps = Long.parseLong(value.trim());
                        builder.append("qps:").append(redisInfo.qps).append("\r\n");
                    } catch (Exception ignore) {
                    }
                    continue;
                }
                if (value != null) {
                    builder.append(key).append(":").append(value).append("\r\n");
                }
                if (key.equals("connected_slaves")) {
                    String connectedSlaves = map.get("connected_slaves");
                    if (connectedSlaves != null) {
                        int i = Integer.parseInt(connectedSlaves.trim());
                        for (int j=0; j<i*2; j++) {
                            String slaveKey = "slave" + j;
                            String slave = map.get(slaveKey);
                            if (slave != null) {
                                builder.append(slaveKey).append(":").append(slave).append("\r\n");
                            }
                        }
                    }
                }
                if (key.equals("maxmemory_human")) {
                    String maxmemory = map.get("maxmemory");
                    String usedMemory = map.get("used_memory");
                    if (maxmemory != null && usedMemory != null) {
                        try {
                            long max = Long.parseLong(maxmemory.trim());
                            double memeoryUsedRate = 0.0;
                            if (max != 0) {
                                memeoryUsedRate = ((double) Long.parseLong(usedMemory.trim())) / max;
                            }
                            builder.append("memory_used_rate:").append(memeoryUsedRate).append("\r\n");
                            builder.append("memory_used_rate_human:").append(String.format("%.2f", memeoryUsedRate * 100.0)).append("%").append("\r\n");
                        } catch (Exception ignore) {
                        }
                        try {
                            redisInfo.maxMemory = Long.parseLong(map.get("maxmemory").trim());
                            redisInfo.usedMemory = Long.parseLong(map.get("used_memory").trim());
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
            redisInfo.string = builder.toString();
            return redisInfo;
        }
    }

    private static final List<String> redisClusterInfoKeys = new ArrayList<>();
    static {
        redisClusterInfoKeys.add("cluster_state");
        redisClusterInfoKeys.add("cluster_slots_assigned");
        redisClusterInfoKeys.add("cluster_slots_ok");
        redisClusterInfoKeys.add("cluster_slots_pfail");
        redisClusterInfoKeys.add("cluster_slots_fail");
        redisClusterInfoKeys.add("cluster_known_nodes");
        redisClusterInfoKeys.add("cluster_size");
    }
    private static String getRedisClusterInfo(RedisClusterResource redisClusterResource) {
        List<RedisClusterResource.Node> nodes = redisClusterResource.getNodes();
        String password = redisClusterResource.getPassword();
        String userName = redisClusterResource.getUserName();
        for (RedisClusterResource.Node node : nodes) {
            Map<String, String> map = getInfoMap(node.getHost(), node.getPort(), userName, password, new byte[][]{Utils.stringToBytes("cluster"), Utils.stringToBytes("info")});
            if (map == null) continue;
            StringBuilder builder = new StringBuilder();
            for (String key : redisClusterInfoKeys) {
                String value = map.get(key);
                if (value != null) {
                    builder.append(key).append(":").append(value).append("\r\n");
                }
            }
            return builder.toString();
        }
        return null;
    }

    private static Map<String, String> getInfoMap(String host, int port, String userName, String password, byte[][] command) {
        RedisClient redisClient = null;
        try {
            redisClient = RedisClientHub.newClient(host, port, userName, password);
            if (redisClient != null) {
                Map<String, String> map = new HashMap<>();
                CompletableFuture<Reply> future = redisClient.sendCommand(command);
                Reply reply = future.get(10, TimeUnit.SECONDS);
                if (reply instanceof BulkReply) {
                    String string = Utils.bytesToString(((BulkReply) reply).getRaw());
                    String[] split = string.split("\r\n");
                    for (String line : split) {
                        if (line.startsWith("#")) continue;
                        String[] split1 = line.split(":");
                        if (split1.length >= 2) {
                            map.put(split1[0], split1[1]);
                        }
                    }
                }
                return map;
            }
            return null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        } finally {
            if (redisClient != null) {
                redisClient.stop(true);
            }
        }
    }

    private static class RedisSentinelInfo {
        RedisInfo redisInfo;
        RedisClientAddr master;
    }
    private static RedisSentinelInfo getRedisSentinelInfo(List<RedisSentinelResource.Node> sentinels, String userName, String password, String masterName) {
        RedisSentinelInfo redisSentinelInfo = new RedisSentinelInfo();
        for (RedisSentinelResource.Node node : sentinels) {
            HostAndPort master = getRedisSentinelMaster(node.getHost(), node.getPort(), masterName);
            if (master != null) {
                RedisClientAddr addr = new RedisClientAddr(master.getHost(), master.getPort(), userName, password);
                redisSentinelInfo.master = addr;
                RedisInfo redisInfo = getRedisInfo(addr);
                if (redisInfo != null) {
                    redisSentinelInfo.redisInfo = redisInfo;
                    return redisSentinelInfo;
                }
            }
        }
        return redisSentinelInfo;
    }

    private static HostAndPort getRedisSentinelMaster(String host, int port, String masterName) {
        RedisClient redisClient = null;
        try {
            redisClient = RedisClientHub.newClient(host, port, null, null);
            if (redisClient != null) {
                CompletableFuture<Reply> future1 = redisClient.sendCommand(RedisCommand.SENTINEL.raw(),
                        RedisSentinelUtils.SENTINEL_GET_MASTER_ADDR_BY_NAME, Utils.stringToBytes(masterName));
                Reply getMasterReply = future1.get(10, TimeUnit.SECONDS);
                return RedisSentinelUtils.processMasterGet(getMasterReply);
            }
            return null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        } finally {
            if (redisClient != null) {
                redisClient.stop(true);
            }
        }
    }

    /**
     * 如果master没有slave，或者有一个ip的master节点占了所有master节点的一半以上，则认为集群是不安全的
     */
    private static boolean redisClusterSafety(List<ClusterNodeInfo> clusterNodeInfos) {
        try {
            Map<String, AtomicLong> map = new HashMap<>();
            for (ClusterNodeInfo clusterNodeInfo : clusterNodeInfos) {
                if (clusterNodeInfo.slaves == null || clusterNodeInfo.slaves.isEmpty()) {
                    return false;
                }
                String[] split = clusterNodeInfo.master.split(":");
                String masterIp = split[0];
                AtomicLong count = map.get(masterIp);
                if (count == null) {
                    count = new AtomicLong();
                    map.put(masterIp, count);
                }
                count.incrementAndGet();
            }
            int size = clusterNodeInfos.size();
            int halfSize;
            if (size % 2 == 0) {
                halfSize = size / 2;
            } else {
                halfSize = (size / 2) + 1;
            }
            for (Map.Entry<String, AtomicLong> entry : map.entrySet()) {
                if (entry.getValue().get() >= halfSize) return false;
            }
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    private static List<ClusterNodeInfo> getRedisClusterNodeInfo(RedisClusterResource redisClusterResource) {
        List<RedisClusterResource.Node> nodes = redisClusterResource.getNodes();
        String password = redisClusterResource.getPassword();
        String userName = redisClusterResource.getUserName();
        for (RedisClusterResource.Node node : nodes) {
            List<ClusterNodeInfo> clusterNodeInfos = clusterNodes(node.getHost(), node.getPort(), userName, password);
            if (clusterNodeInfos != null) {
                return clusterNodeInfos;
            }
        }
        return null;
    }

    private static class ClusterNodeInfo {
        String master;
        List<String> slaves = new ArrayList<>();
        String slots;
    }

    private static List<ClusterNodeInfo> clusterNodes(String host, int port, String userName, String password) {
        RedisClient redisClient = null;
        try {
            redisClient = RedisClientHub.newClient(host, port, userName, password);
            if (redisClient != null) {
                CompletableFuture<Reply> future = redisClient.sendCommand(Utils.stringToBytes("cluster"), Utils.stringToBytes("nodes"));
                Reply reply = future.get(10, TimeUnit.SECONDS);
                if (reply instanceof BulkReply) {
                    String string = Utils.bytesToString(((BulkReply) reply).getRaw());
                    string = string.replaceAll("myself,", "");
                    String[] split = string.split("\n");
                    Map<String, ClusterNodeInfo> map = new HashMap<>();
                    for (String subStr : split) {
                        if (subStr.contains("master")) {
                            String[] s = subStr.split(" ");
                            ClusterNodeInfo nodeInfo = new ClusterNodeInfo();
                            nodeInfo.master = s[1];
                            nodeInfo.slots = s[8];
                            map.put(s[0], nodeInfo);
                        }
                    }
                    for (String subStr : split) {
                        if (subStr.contains("slave")) {
                            String[] s = subStr.split(" ");
                            String masterId = s[3];
                            ClusterNodeInfo nodeInfo = map.get(masterId);
                            nodeInfo.slaves.add(s[1]);
                        }
                    }
                    return new ArrayList<>(map.values());
                }
            }
            return null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        } finally {
            if (redisClient != null) {
                redisClient.stop(true);
            }
        }
    }

    private static Map<String, String> parseKv(String str) {
        if (str == null) return null;
        String[] split = str.split("\r\n");
        Map<String, String> map = new HashMap<>();
        for (String s : split) {
            String[] split1 = s.split(":");
            if (split1.length == 2) {
                map.put(split1[0].trim(), split1[1].trim());
            }
        }
        return map;
    }
}
