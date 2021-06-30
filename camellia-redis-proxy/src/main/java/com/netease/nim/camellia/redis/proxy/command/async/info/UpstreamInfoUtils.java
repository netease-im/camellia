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
            Set<Resource> resources = ResourceUtil.getAllResources(resourceTable);
            builder.append("upstream_cluster_count:").append(resources.size()).append("\n");
            builder.append("\n");
            int i=0;
            for (Resource resource : resources) {
                builder.append("## Upstream").append(i).append("\n");
                i ++;
                builder.append("url:").append(PasswordMaskUtils.maskResource(resource.getUrl())).append("\n");
                Resource redisResource = RedisResourceUtil.parseResourceByUrl(resource);
                if (redisResource instanceof RedisResource) {
                    RedisClientAddr addr = new RedisClientAddr(((RedisResource) redisResource).getHost(),
                            ((RedisResource) redisResource).getPort(), ((RedisResource) redisResource).getPassword());
                    builder.append("### redis-node-info").append("\n");
                    String redisInfo = getRedisInfo(addr);
                    if (redisInfo != null) {
                        builder.append(redisInfo);
                    }
                } else if (redisResource instanceof RedisSentinelResource) {
                    builder.append("### redis-node-info").append("\n");
                    String redisSentinelInfo = getRedisSentinelInfo(((RedisSentinelResource) redisResource).getNodes(),
                            ((RedisSentinelResource) redisResource).getPassword(), ((RedisSentinelResource) redisResource).getMaster());
                    builder.append(redisSentinelInfo);
                } else if (redisResource instanceof RedisSentinelSlavesResource) {
                    builder.append("### redis-node-info").append("\n");
                    String redisSentinelInfo = getRedisSentinelInfo(((RedisSentinelSlavesResource) redisResource).getNodes(),
                            ((RedisSentinelSlavesResource) redisResource).getPassword(), ((RedisSentinelSlavesResource) redisResource).getMaster());
                    builder.append(redisSentinelInfo);
                } else if (redisResource instanceof RedisClusterResource) {
                    String redisClusterInfo = getRedisClusterInfo((RedisClusterResource) redisResource);
                    if (redisClusterInfo != null) {
                        builder.append("### redis-cluster-info").append("\n");
                        builder.append(redisClusterInfo);
                    }
                    List<ClusterNodeInfo> redisClusterNodeInfo = getRedisClusterNodeInfo((RedisClusterResource) redisResource);
                    if (redisClusterNodeInfo != null) {
                        builder.append("### redis-cluster-node-info").append("\n");
                        int j = 0;
                        for (ClusterNodeInfo clusterNodeInfo : redisClusterNodeInfo) {
                            builder.append("node").append(j).append(":").append("master=").append(clusterNodeInfo.master)
                                    .append(",slave=").append(clusterNodeInfo.slaves)
                                    .append(",slots=").append(clusterNodeInfo.slots).append("\n");
                            j++;
                        }
                        builder.append("### redis-node-info").append("\n");
                        int k = 0;
                        for (ClusterNodeInfo clusterNodeInfo : redisClusterNodeInfo) {
                            String master = clusterNodeInfo.master;
                            String[] split = master.split("@");
                            String[] split1 = split[0].split(":");
                            String host = split1[0];
                            int port = Integer.parseInt(split1[1]);
                            String redisInfo = getRedisInfo(new RedisClientAddr(host, port, ((RedisClusterResource) redisResource).getPassword()));
                            builder.append("#### node").append(k).append("\n");
                            builder.append("master_url=").append(clusterNodeInfo.master).append("\n");
                            builder.append(redisInfo);
                            k ++;
                        }
                    }
                }
                builder.append("\n");
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
    }
    private static String getRedisInfo(RedisClientAddr addr) {
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
                if (key.equals("maxmemory_human")) {
                    String maxmemory = map.get("maxmemory");
                    String usedMemory = map.get("used_memory");
                    if (maxmemory != null && usedMemory != null) {
                        try {
                            double memeoryUsedRate = ((double) Long.parseLong(usedMemory.trim())) / Long.parseLong(maxmemory.trim());
                            builder.append("memory_used_rate:").append(memeoryUsedRate).append("\n");
                            builder.append("memory_used_rate_hum:").append(String.format("%.2f", memeoryUsedRate / 100.0)).append("%").append("\n");
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
            return builder.toString();
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

    private static String getRedisSentinelInfo(List<RedisSentinelResource.Node> sentinels, String password, String masterName) {
        StringBuilder builder = new StringBuilder();
        for (RedisSentinelResource.Node node : sentinels) {
            HostAndPort master = getRedisSentinelMaster(node.getHost(), node.getPort(), masterName);
            if (master != null) {
                RedisClientAddr addr = new RedisClientAddr(master.getHost(), master.getPort(), password);
                String redisInfo = getRedisInfo(addr);
                if (redisInfo != null) {
                    builder.append("master_url:").append(PasswordMaskUtils.maskAddr(addr.getUrl())).append("\n");
                    builder.append(redisInfo);
                    return builder.toString();
                }
            }
        }
        return null;
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
