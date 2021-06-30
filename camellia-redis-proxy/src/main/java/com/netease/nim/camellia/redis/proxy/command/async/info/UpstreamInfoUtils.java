package com.netease.nim.camellia.redis.proxy.command.async.info;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceUtil;
import com.netease.nim.camellia.redis.proxy.command.async.*;
import com.netease.nim.camellia.redis.proxy.command.async.sentinel.RedisSentinelUtils;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.redis.resource.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2021/6/30
 */
public class UpstreamInfoUtils {

    private static final Logger logger = LoggerFactory.getLogger(UpstreamInfoUtils.class);

    public static String upstreamInfo(Long bid, String bgroup, AsyncCamelliaRedisTemplateChooser chooser) {
        try {
            AsyncCamelliaRedisTemplate template = chooser.choose(bid, bgroup);
            if (template == null) {
                return null;
            }
            ResourceTable resourceTable = template.getResourceTable();
            StringBuilder builder = new StringBuilder();
            builder.append("# Upstream-Info").append("\n");
            builder.append("route_conf:").append(ReadableResourceTableUtil.readableResourceTable(PasswordMaskUtils.maskResourceTable(resourceTable))).append("\n");

            if (bid != null && bgroup != null) {
                builder.append("bid:").append(bid).append("\n");
                builder.append("bgroup:").append(bgroup).append("\n");
            }
            List<Resource> resources = new ArrayList<>(ResourceUtil.getAllResources(resourceTable));
            builder.append("upstream_cluster_count:").append(resources.size()).append("\n");
            for (int i=0; i<resources.size(); i++) {
                Resource resource = resources.get(i);
                builder.append("upstream").append(i).append("_url:").append(resource.getUrl()).append("\n");
            }
            builder.append("\n");
            for (int i=0; i<resources.size(); i++) {
                Resource resource = resources.get(i);
                builder.append("## Upstream").append(i).append("\n");
                builder.append("url:").append(PasswordMaskUtils.maskResource(resource.getUrl())).append("\n");
                Resource redisResource = RedisResourceUtil.parseResourceByUrl(resource);
                if (redisResource instanceof RedisResource) {
                    RedisClientAddr addr = new RedisClientAddr(((RedisResource) redisResource).getHost(),
                            ((RedisResource) redisResource).getPort(), ((RedisResource) redisResource).getPassword());
                    builder.append("### redis-node-info").append("\n");
                    RedisInfo redisInfo = getRedisInfo(addr);
                    if (redisInfo != null) {
                        builder.append(redisInfo.string);
                    }
                } else if (redisResource instanceof RedisSentinelResource) {
                    builder.append("### redis-node-info").append("\n");
                    RedisSentinelInfo redisSentinelInfo = getRedisSentinelInfo(((RedisSentinelResource) redisResource).getNodes(),
                            ((RedisSentinelResource) redisResource).getPassword(), ((RedisSentinelResource) redisResource).getMaster());
                    if (redisSentinelInfo.master != null) {
                        builder.append("master_url:").append(redisSentinelInfo.master).append("\n");
                    }
                    if (redisSentinelInfo.redisInfo != null && redisSentinelInfo.redisInfo.string != null) {
                        builder.append(redisSentinelInfo.redisInfo.string);
                    }
                } else if (redisResource instanceof RedisSentinelSlavesResource) {
                    builder.append("### redis-node-info").append("\n");
                    RedisSentinelInfo redisSentinelInfo = getRedisSentinelInfo(((RedisSentinelSlavesResource) redisResource).getNodes(),
                            ((RedisSentinelSlavesResource) redisResource).getPassword(), ((RedisSentinelSlavesResource) redisResource).getMaster());
                    if (redisSentinelInfo.master != null) {
                        builder.append("master_url:").append(redisSentinelInfo.master).append("\n");
                    }
                    if (redisSentinelInfo.redisInfo != null && redisSentinelInfo.redisInfo.string != null) {
                        builder.append(redisSentinelInfo.redisInfo.string);
                    }
                } else if (redisResource instanceof RedisClusterResource) {
                    String redisClusterInfo = getRedisClusterInfo((RedisClusterResource) redisResource);
                    if (redisClusterInfo != null) {
                        builder.append("### redis-cluster-info").append("\n");
                        builder.append(redisClusterInfo);
                    }
                    long totalMaxMemory = 0;
                    long totalUsedMemory = 0;
                    StringBuilder clusterBuilder = new StringBuilder();
                    List<ClusterNodeInfo> redisClusterNodeInfo = getRedisClusterNodeInfo((RedisClusterResource) redisResource);
                    if (redisClusterNodeInfo != null) {
                        StringBuilder redisNodeInfoBuilder = new StringBuilder();
                        redisNodeInfoBuilder.append("### redis-node-info").append("\n");
                        int k = 0;
                        Map<String, RedisInfo> map = new HashMap<>();
                        for (ClusterNodeInfo clusterNodeInfo : redisClusterNodeInfo) {
                            String master = clusterNodeInfo.master;
                            String[] split = master.split("@");
                            String[] split1 = split[0].split(":");
                            String host = split1[0];
                            int port = Integer.parseInt(split1[1]);
                            RedisInfo redisInfo = getRedisInfo(new RedisClientAddr(host, port, ((RedisClusterResource) redisResource).getPassword()));
                            redisNodeInfoBuilder.append("#### node").append(k).append("\n");
                            redisNodeInfoBuilder.append("master_url=").append(clusterNodeInfo.master).append("\n");
                            if (redisInfo != null) {
                                redisNodeInfoBuilder.append(redisInfo.string);
                                totalMaxMemory += redisInfo.maxMemory;
                                totalUsedMemory += redisInfo.usedMemory;
                                map.put(master, redisInfo);
                            }
                            k ++;
                        }

                        StringBuilder redisClusterNodeInfoBuilder = new StringBuilder();
                        redisClusterNodeInfoBuilder.append("### redis-cluster-node-info").append("\n");
                        int j = 0;
                        for (ClusterNodeInfo clusterNodeInfo : redisClusterNodeInfo) {
                            redisClusterNodeInfoBuilder.append("node").append(j).append(":").append("master=").append(clusterNodeInfo.master)
                                    .append(",slave=").append(clusterNodeInfo.slaves)
                                    .append(",slots=").append(clusterNodeInfo.slots);
                            RedisInfo redisInfo = map.get(clusterNodeInfo.master);
                            if (redisInfo != null) {
                                redisClusterNodeInfoBuilder.append(",maxMemory=").append(humanReadableByteCountBin(redisInfo.maxMemory));
                                redisClusterNodeInfoBuilder.append(",usedMemory=").append(humanReadableByteCountBin(redisInfo.usedMemory));
                                double rate = 0.0;
                                if (redisInfo.maxMemory != 0) {
                                    rate = (double) redisInfo.usedMemory / redisInfo.maxMemory;
                                }
                                redisClusterNodeInfoBuilder.append(",memoryUsedRate=").append(String.format("%.2f", rate * 100.0)).append("%");
                            }
                            redisClusterNodeInfoBuilder.append("\n");
                            j++;
                        }
                        clusterBuilder.append(redisClusterNodeInfoBuilder).append(redisNodeInfoBuilder);
                    }
                    double clusterMemoryUsedRate = 0.0;
                    if (totalMaxMemory != 0) {
                        clusterMemoryUsedRate = (double) totalUsedMemory / totalMaxMemory;
                    }
                    builder.append("cluster_maxmemory:").append(totalMaxMemory).append("\n");
                    builder.append("cluster_maxmemory_human:").append(humanReadableByteCountBin(totalMaxMemory)).append("\n");
                    builder.append("cluster_used_memory:").append(totalUsedMemory).append("\n");
                    builder.append("cluster_used_memory_human:").append(humanReadableByteCountBin(totalUsedMemory)).append("\n");
                    builder.append("cluster_memory_used_rate:").append(clusterMemoryUsedRate).append("\n");
                    builder.append("cluster_memory_used_rate_human:").append(String.format("%.2f", clusterMemoryUsedRate * 100.0)).append("%").append("\n");
                    builder.append(clusterBuilder);
                }
                builder.append("\n");
            }
            return builder.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
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
    }

    private static class RedisInfo {
        private String string;
        private long maxMemory;
        private long usedMemory;
    }
    private static RedisInfo getRedisInfo(RedisClientAddr addr) {
        Map<String, String> map = getInfoMap(addr.getHost(), addr.getPort(), addr.getPassword(), new byte[][] {RedisCommand.INFO.raw()});
        if (map == null) {
            return null;
        } else {
            StringBuilder builder = new StringBuilder();
            for (String key : redisInfoKeys) {
                String value = map.get(key);
                if (value != null) {
                    builder.append(key).append(":").append(value).append("\n");
                }
                if (key.equals("connected_slaves")) {
                    String connectedSlaves = map.get("connected_slaves");
                    if (connectedSlaves != null) {
                        int i = Integer.parseInt(connectedSlaves.trim());
                        for (int j=0; j<i*2; j++) {
                            String slaveKey = "slave" + j;
                            String slave = map.get(slaveKey);
                            if (slave != null) {
                                builder.append(slaveKey).append(":").append(slave).append("\n");
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
                            builder.append("memory_used_rate:").append(memeoryUsedRate).append("\n");
                            builder.append("memory_used_rate_human:").append(String.format("%.2f", memeoryUsedRate * 100.0)).append("%").append("\n");
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
            String string = builder.toString();
            RedisInfo redisInfo = new RedisInfo();
            redisInfo.string = string;
            try {
                redisInfo.maxMemory = Long.parseLong(map.get("maxmemory").trim());
                redisInfo.usedMemory = Long.parseLong(map.get("used_memory").trim());
            } catch (Exception ignore) {
            }
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
        for (RedisClusterResource.Node node : nodes) {
            Map<String, String> map = getInfoMap(node.getHost(), node.getPort(), password, new byte[][]{Utils.stringToBytes("cluster"), Utils.stringToBytes("info")});
            if (map == null) continue;
            StringBuilder builder = new StringBuilder();
            for (String key : redisClusterInfoKeys) {
                String value = map.get(key);
                if (value != null) {
                    builder.append(key).append(":").append(value).append("\n");
                }
            }
            return builder.toString();
        }
        return null;
    }

    private static Map<String, String> getInfoMap(String host, int port, String password, byte[][] command) {
        RedisClient redisClient = null;
        try {
            redisClient = RedisClientHub.newClient(host, port, password);
            if (redisClient != null) {
                Map<String, String> map = new HashMap<>();
                CompletableFuture<Reply> future = redisClient.sendCommand(command);
                Reply reply = future.get(10, TimeUnit.SECONDS);
                if (reply instanceof BulkReply) {
                    String string = Utils.bytesToString(((BulkReply) reply).getRaw());
                    String[] split = string.split("\n");
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
    private static RedisSentinelInfo getRedisSentinelInfo(List<RedisSentinelResource.Node> sentinels, String password, String masterName) {
        RedisSentinelInfo redisSentinelInfo = new RedisSentinelInfo();
        for (RedisSentinelResource.Node node : sentinels) {
            HostAndPort master = getRedisSentinelMaster(node.getHost(), node.getPort(), masterName);
            if (master != null) {
                RedisClientAddr addr = new RedisClientAddr(master.getHost(), master.getPort(), password);
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
            redisClient = RedisClientHub.newClient(host, port, null);
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

    private static List<ClusterNodeInfo> getRedisClusterNodeInfo(RedisClusterResource redisClusterResource) {
        List<RedisClusterResource.Node> nodes = redisClusterResource.getNodes();
        String password = redisClusterResource.getPassword();
        for (RedisClusterResource.Node node : nodes) {
            List<ClusterNodeInfo> clusterNodeInfos = clusterNodes(node.getHost(), node.getPort(), password);
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

    private static List<ClusterNodeInfo> clusterNodes(String host, int port, String password) {
        RedisClient redisClient = null;
        try {
            redisClient = RedisClientHub.newClient(host, port, password);
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

}
