package com.netease.nim.camellia.redis.jediscluster;

import redis.clients.jedis.Connection;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2024/11/11
 */
public class JedisClusterSlaveUtils {

    public static Map<Integer, List<HostAndPort>> clusterSlotsSlaveNodes(Connection connection) {
        Map<Integer, List<HostAndPort>> slotSlaveMap = new HashMap<>();
        connection.sendCommand(Protocol.Command.CLUSTER, "SLOTS");
        List<Object> slots = connection.getObjectMultiBulkReply();
        for (Object slotInfoObj : slots) {
            List<Object> slotInfo = (List<Object>) slotInfoObj;
            if (slotInfo.size() >= 3) {
                List<Integer> slotArray = getAssignedSlotArray(slotInfo);
                List<HostAndPort> slaveNodes = new ArrayList<>();
                for (int i = 3; i < slotInfo.size(); i++) {
                    List<Object> hostInfos = (List<Object>) slotInfo.get(i);
                    HostAndPort targetNode = generateHostAndPort(hostInfos);
                    slaveNodes.add(targetNode);
                }
                for (Integer slot : slotArray) {
                    slotSlaveMap.put(slot, slaveNodes);
                }
            }
        }
        return slotSlaveMap;
    }

    private static HostAndPort generateHostAndPort(List<Object> hostInfos) {
        return new HostAndPort(SafeEncoder.encode((byte[]) hostInfos.get(0)), ((Long) hostInfos.get(1)).intValue());
    }

    private static List<Integer> getAssignedSlotArray(List<Object> slotInfo) {
        List<Integer> slotNums = new ArrayList<>();
        for (int slot = ((Long) slotInfo.get(0)).intValue(); slot <= ((Long) slotInfo.get(1)).intValue(); slot++) {
            slotNums.add(slot);
        }
        return slotNums;
    }
}
