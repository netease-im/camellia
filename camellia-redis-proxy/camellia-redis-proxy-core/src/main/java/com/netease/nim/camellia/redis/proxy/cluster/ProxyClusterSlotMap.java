package com.netease.nim.camellia.redis.proxy.cluster;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.MD5Util;
import com.netease.nim.camellia.tools.utils.Pair;

import java.util.*;

/**
 * Created by caojiajun on 2024/6/17
 */
public class ProxyClusterSlotMap {

    private final String md5;
    private final ProxyNode currentNode;
    private final ProxyNode[] slotArray;
    private boolean currentNodeOnline;
    private final List<ProxyNode> onlineNodes;
    private final Set<ProxyNode> onlineNodeSet;

    public ProxyClusterSlotMap(ProxyNode currentNode, ProxyNode[] slotArray) {
        if (currentNode == null) {
            throw new IllegalArgumentException("current node is null");
        }
        this.currentNode = currentNode;
        this.slotArray = slotArray;
        if (slotArray.length != RedisClusterCRC16Utils.SLOT_SIZE) {
            throw new IllegalArgumentException("slot array illegal size");
        }
        Set<ProxyNode> set = new HashSet<>();
        for (ProxyNode proxyNode : slotArray) {
            if (proxyNode == null) {
                throw new IllegalArgumentException("slot array illegal node");
            }
            if (proxyNode.equals(currentNode)) {
                currentNodeOnline = true;
            }
            set.add(proxyNode);
        }
        this.onlineNodes = new ArrayList<>(set);
        this.onlineNodeSet = new HashSet<>(set);
        Collections.sort(onlineNodes);
        JSONObject json = toJson();
        json.remove("currentNode");
        this.md5 = MD5Util.md5(json.toString());
    }

    public boolean isCurrentNodeOnline() {
        return currentNodeOnline;
    }

    public boolean isOnlineNodesEmpty() {
        return onlineNodes.isEmpty();
    }

    public List<ProxyNode> getOnlineNodes() {
        return new ArrayList<>(onlineNodes);
    }

    public ProxyNode getCurrentNode() {
        return currentNode;
    }

    public ProxyNode getBySlot(int slot) {
        return slotArray[slot];
    }

    public boolean isSlotInCurrentNode(int slot) {
        if (!currentNodeOnline) {
            return false;
        }
        return slotArray[slot].equals(currentNode);
    }

    public ProxyNode[] getSlotArray() {
        ProxyNode[] array = new ProxyNode[slotArray.length];
        System.arraycopy(slotArray, 0, array, 0, array.length);
        return array;
    }

    public boolean contains(ProxyNode proxyNode) {
        return onlineNodeSet.contains(proxyNode);
    }

    public Map<ProxyNode, List<Integer>> getNodeSlotMap() {
        Map<ProxyNode, List<Integer>> map = new HashMap<>();
        for (int i=0; i<slotArray.length; i++) {
            ProxyNode proxyNode = slotArray[i];
            List<Integer> slots = map.computeIfAbsent(proxyNode, node -> new ArrayList<>());
            slots.add(i);
        }
        return map;
    }

    public String getMd5() {
        return md5;
    }

    public Reply clusterInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append("cluster_state:ok").append("\r\n");
        builder.append("cluster_slots_assigned:16384").append("\r\n");
        builder.append("cluster_slots_ok:16384").append("\r\n");
        builder.append("cluster_slots_pfail:0").append("\r\n");
        builder.append("cluster_slots_fail:0").append("\r\n");
        List<ProxyNode> nodes = getOnlineNodes();
        int size = nodes.size();
        builder.append("cluster_known_nodes:").append(size).append("\r\n");
        builder.append("cluster_size:").append(size).append("\r\n");
        builder.append("cluster_current_epoch:").append(size).append("\r\n");
        int myEpoch = 1;
        int i=0;
        for (ProxyNode proxyNode : nodes) {
            i ++;
            if (proxyNode.equals(currentNode)) {
                myEpoch = i;
                break;
            }
        }
        builder.append("cluster_my_epoch:").append(myEpoch).append("\r\n");
        builder.append("cluster_stats_messages_sent:0").append("\r\n");
        builder.append("cluster_stats_messages_received:0").append("\r\n");
        builder.append("total_cluster_links_buffer_limit_exceeded:0").append("\r\n");
        String str = builder.toString();
        return new BulkReply(Utils.stringToBytes(str));
    }

    public Reply clusterNodes() {
        StringBuilder builder = new StringBuilder();
        Map<ProxyNode, List<Integer>> map = getNodeSlotMap();
        int i=1;
        for (Map.Entry<ProxyNode, List<Integer>> entry : map.entrySet()) {
            ProxyNode proxyNode = entry.getKey();
            builder.append(MD5Util.md5(proxyNode.toString())).append(" ");
            builder.append(proxyNode.getHost()).append(":").append(proxyNode.getPort()).append("@").append(proxyNode.getPort() + 1000).append(" ");
            if (proxyNode.equals(currentNode)) {
                builder.append("myself,master").append(" ");
            } else {
                builder.append("master").append(" ");
            }
            builder.append("-").append(" ");
            builder.append("0").append(" ");
            builder.append(System.currentTimeMillis()).append(" ");
            builder.append(i).append(" ");
            builder.append("connected").append(" ");
            List<Pair<Integer, Integer>> pairs = SlotSplitUtils.splitSlots(entry.getValue());
            for (Pair<Integer, Integer> pair : pairs) {
                if (Objects.equals(pair.getFirst(), pair.getSecond())) {
                    builder.append(pair.getFirst());
                } else {
                    builder.append(pair.getFirst()).append("-").append(pair.getSecond());
                }
                builder.append(" ");
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append("\n");
            i++;
        }
        String str = builder.toString();
        return new BulkReply(Utils.stringToBytes(str));
    }

    public Reply clusterSlots() {
        List<MultiBulkReply> replies = new ArrayList<>();
        Map<ProxyNode, List<Integer>> map = getNodeSlotMap();
        for (Map.Entry<ProxyNode, List<Integer>> entry : map.entrySet()) {
            ProxyNode proxyNode = entry.getKey();
            List<Integer> slots = entry.getValue();
            List<Pair<Integer, Integer>> pairs = SlotSplitUtils.splitSlots(slots);
            for (Pair<Integer, Integer> pair : pairs) {
                BulkReply host = new BulkReply(Utils.stringToBytes(proxyNode.getHost()));
                IntegerReply port = new IntegerReply((long) proxyNode.getPort());
                BulkReply id = new BulkReply(Utils.stringToBytes(MD5Util.md5(proxyNode.toString())));
                MultiBulkReply master = new MultiBulkReply(new Reply[]{host, port, id});
                IntegerReply start = new IntegerReply(pair.getFirst().longValue());
                IntegerReply stop = new IntegerReply(pair.getSecond().longValue());
                replies.add(new MultiBulkReply(new Reply[]{start, stop, master}));
            }
        }
        return new MultiBulkReply(replies.toArray(new MultiBulkReply[0]));
    }

    public JSONObject toJson() {
        Map<ProxyNode, List<Integer>> map = getNodeSlotMap();
        JSONObject data = new JSONObject(true);
        data.put("currentNode", currentNode.toString());
        JSONObject json = new JSONObject(true);
        for (ProxyNode node : onlineNodes) {
            List<Integer> integers = map.get(node);
            List<Pair<Integer, Integer>> pairs = SlotSplitUtils.splitSlots(integers);
            JSONArray slot = new JSONArray();
            for (Pair<Integer, Integer> pair : pairs) {
                if (pair.getFirst().equals(pair.getSecond())) {
                    slot.add(pair.getFirst().toString());
                } else {
                    slot.add(pair.getFirst() + "-" + pair.getSecond());
                }
            }
            json.put(node.toString(), slot);
        }
        data.put("onlineNodes", json);
        return data;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public static ProxyClusterSlotMap parseString(String str) {
        JSONObject jsonObject = JSONObject.parseObject(str);
        String string = jsonObject.getString("currentNode");
        ProxyNode currentNode = ProxyNode.parseString(string);
        JSONObject onlineNodesJson = jsonObject.getJSONObject("onlineNodes");
        ProxyNode[] slotArray = new ProxyNode[RedisClusterCRC16Utils.SLOT_SIZE];
        for (Map.Entry<String, Object> entry : onlineNodesJson.entrySet()) {
            String key = entry.getKey();
            ProxyNode proxyNode = ProxyNode.parseString(key);
            JSONArray array = JSONArray.parseArray(entry.getValue().toString());
            for (Object o : array) {
                String slot = String.valueOf(o);
                if (slot.contains("-")) {
                    String[] split = slot.split("-");
                    int start = Integer.parseInt(split[0]);
                    int stop = Integer.parseInt(split[1]);
                    for (int i=start; i<=stop; i++) {
                        slotArray[i] = proxyNode;
                    }
                } else {
                    slotArray[Integer.parseInt(slot)] = proxyNode;
                }
            }
        }
        return new ProxyClusterSlotMap(currentNode, slotArray);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProxyClusterSlotMap) {
            return toString().equals(obj.toString());
        } else {
            return false;
        }
    }
}
