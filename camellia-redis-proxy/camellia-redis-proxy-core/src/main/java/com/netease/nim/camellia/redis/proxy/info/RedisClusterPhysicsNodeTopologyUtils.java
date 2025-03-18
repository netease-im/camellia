package com.netease.nim.camellia.redis.proxy.info;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.resource.RedisClusterResource;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by caojiajun on 2025/3/18
 */
public class RedisClusterPhysicsNodeTopologyUtils {

    public static String toExcel(Map<String, String> urlMap) {
        RedisPhysicsNodeTopology topology = RedisClusterPhysicsNodeTopologyUtils.topology(urlMap.keySet());
        List<RedisPhysicsNodeTopology.PhysicsNodeView> physicsNodeViewList = topology.physicsNodeViewList();
        Map<String, Integer> nodeIndexMap = new HashMap<>();
        for (int i=0; i<physicsNodeViewList.size(); i++) {
            nodeIndexMap.put(physicsNodeViewList.get(i).host(), i);
        }
        String[][] data = new String[topology.redisClusterViewList().size() + 1][nodeIndexMap.size()*2 + 5];
        String[] title = data[0];
        title[0] = "url";
        title[1] = "name";
        title[2] = "size";
        title[3] = "nodes";
        for (Map.Entry<String, Integer> entry : nodeIndexMap.entrySet()) {
            title[entry.getValue()*2 + 4] = entry.getKey();
        }
        int i=1;
        for (RedisPhysicsNodeTopology.RedisClusterView redisClusterView : topology.redisClusterViewList()) {
            String[] row = data[i];
            row[0] = redisClusterView.url();
            row[1] = urlMap.get(redisClusterView.url());
            row[2] = String.valueOf(redisClusterView.size());
            row[3] = String.valueOf(redisClusterView.nodes());
            for (RedisPhysicsNodeTopology.NodeInfo nodeInfo : redisClusterView.nodeInfoList()) {
                Integer index = nodeIndexMap.get(nodeInfo.host());
                row[index*2 + 4] = String.valueOf(nodeInfo.master());
                row[index*2 + 5] = String.valueOf(nodeInfo.slave());
            }
            i++;
        }

        StringBuilder builder = new StringBuilder();
        for (String[] row : data) {
            for (String cell : row) {
                builder.append(cell == null ? "" : cell).append("\t");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    public static JSONObject topologyJson(Map<String, String> urlMap) {
        RedisPhysicsNodeTopology topology = topology(urlMap.keySet());
        JSONObject json = new JSONObject();
        JSONArray redisClusterViewList = new JSONArray();
        for (RedisPhysicsNodeTopology.RedisClusterView redisClusterView : topology.redisClusterViewList()) {
            JSONObject view = new JSONObject();
            view.put("url", redisClusterView.url());
            view.put("name", urlMap.get(redisClusterView.url()));
            view.put("size", redisClusterView.size());
            view.put("nodes", redisClusterView.nodes());
            JSONArray list = new JSONArray();
            for (RedisPhysicsNodeTopology.NodeInfo nodeInfo : redisClusterView.nodeInfoList()) {
                JSONObject node = new JSONObject();
                node.put("host", nodeInfo.host());
                node.put("master", nodeInfo.master());
                node.put("slave", nodeInfo.slave());
                list.add(node);
            }
            view.put("nodeInfoList", list);
            redisClusterViewList.add(view);
        }
        JSONArray physicsNodeViewList = new JSONArray();
        for (RedisPhysicsNodeTopology.PhysicsNodeView physicsNodeView : topology.physicsNodeViewList()) {
            JSONObject view = new JSONObject();
            view.put("host", physicsNodeView.host());
            view.put("nodes", physicsNodeView.nodes());
            JSONArray list = new JSONArray();
            for (RedisPhysicsNodeTopology.RedisInfo redisInfo : physicsNodeView.clusterInfoList()) {
                JSONObject node = new JSONObject();
                node.put("url", redisInfo.url());
                view.put("name", urlMap.get(redisInfo.url()));
                node.put("master", redisInfo.master());
                node.put("slave", redisInfo.slave());
                list.add(node);
            }
            view.put("clusterInfoList", list);
            physicsNodeViewList.add(view);
        }
        json.put("redisClusterViewList", redisClusterViewList);
        json.put("physicsNodeViewList", physicsNodeViewList);
        return json;
    }

    public static RedisPhysicsNodeTopology topology(Set<String> urlSet) {
        List<RedisPhysicsNodeTopology.RedisClusterView> redisClusterViewList = new ArrayList<>();
        for (String url : urlSet) {
            Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource(url));
            if (!(resource instanceof RedisClusterResource)) {
                throw new IllegalArgumentException("only support redis-cluster");
            }
            redisClusterViewList.add(redisClusterView((RedisClusterResource) resource));
        }
        Map<String, List<RedisPhysicsNodeTopology.RedisInfo>> redisInfoMap = new HashMap<>();
        Map<String, AtomicInteger> nodesMap = new HashMap<>();
        for (RedisPhysicsNodeTopology.RedisClusterView redisClusterView : redisClusterViewList) {
            String url = redisClusterView.url();
            List<RedisPhysicsNodeTopology.NodeInfo> nodeInfos = redisClusterView.nodeInfoList();
            for (RedisPhysicsNodeTopology.NodeInfo nodeInfo : nodeInfos) {
                String host = nodeInfo.host();
                int master = nodeInfo.master();
                int slave = nodeInfo.slave();
                CamelliaMapUtils.computeIfAbsent(nodesMap, host, k -> new AtomicInteger()).addAndGet(master);
                CamelliaMapUtils.computeIfAbsent(nodesMap, host, k -> new AtomicInteger()).addAndGet(slave);

                List<RedisPhysicsNodeTopology.RedisInfo> list = CamelliaMapUtils.computeIfAbsent(redisInfoMap, host, k -> new ArrayList<>());
                list.add(new RedisPhysicsNodeTopology.RedisInfo(url, master, slave));
            }
        }
        List<RedisPhysicsNodeTopology.PhysicsNodeView> physicsNodeViewList = new ArrayList<>();
        for (Map.Entry<String, List<RedisPhysicsNodeTopology.RedisInfo>> entry : redisInfoMap.entrySet()) {
            String host = entry.getKey();
            List<RedisPhysicsNodeTopology.RedisInfo> redisInfoList = entry.getValue();
            int nodes = nodesMap.get(host).get();
            RedisPhysicsNodeTopology.PhysicsNodeView physicsNodeView = new RedisPhysicsNodeTopology.PhysicsNodeView(host, nodes, redisInfoList);
            physicsNodeViewList.add(physicsNodeView);
        }
        return new RedisPhysicsNodeTopology(physicsNodeViewList, redisClusterViewList);
    }

    private static RedisPhysicsNodeTopology.RedisClusterView redisClusterView(RedisClusterResource redisClusterResource) {
        for (RedisClusterResource.Node node : redisClusterResource.getNodes()) {
            List<UpstreamInfoUtils.ClusterNodeInfo> clusterNodeInfos = UpstreamInfoUtils.clusterNodes(redisClusterResource, node.getHost(), node.getPort(), redisClusterResource.getUserName(), redisClusterResource.getPassword());
            if (clusterNodeInfos != null) {
                int size = 0;
                int nodes = 0;
                Map<String, AtomicInteger> masterMap = new HashMap<>();
                Map<String, AtomicInteger> slaveMap = new HashMap<>();
                for (UpstreamInfoUtils.ClusterNodeInfo info : clusterNodeInfos) {
                    size ++;
                    nodes ++;
                    if (info.slaves != null) {
                        nodes += info.slaves.size();
                    }
                    CamelliaMapUtils.computeIfAbsent(masterMap, getHost(info.master), k -> new AtomicInteger()).incrementAndGet();
                    if (info.slaves != null) {
                        for (String slave : info.slaves) {
                            CamelliaMapUtils.computeIfAbsent(slaveMap, getHost(slave), k -> new AtomicInteger()).incrementAndGet();
                        }
                    }
                }
                Set<String> hostSet = new HashSet<>();
                hostSet.addAll(masterMap.keySet());
                hostSet.addAll(slaveMap.keySet());
                List<RedisPhysicsNodeTopology.NodeInfo> nodeInfoList = new ArrayList<>();
                for (String host : hostSet) {
                    int master = masterMap.get(host) == null ? 0: masterMap.get(host).get();
                    int slave = slaveMap.get(host) == null ? 0: slaveMap.get(host).get();
                    RedisPhysicsNodeTopology.NodeInfo nodeInfo = new RedisPhysicsNodeTopology.NodeInfo(host, master, slave);
                    nodeInfoList.add(nodeInfo);
                }
                return new RedisPhysicsNodeTopology.RedisClusterView(redisClusterResource.getUrl(), size, nodes, nodeInfoList);
            }
        }
        return null;
    }

    private static String getHost(String node) {
        return node.split(":")[0];
    }
}
