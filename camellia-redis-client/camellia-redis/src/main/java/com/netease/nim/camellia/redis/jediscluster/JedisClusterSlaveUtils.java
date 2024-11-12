package com.netease.nim.camellia.redis.jediscluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2024/11/11
 */
public class JedisClusterSlaveUtils {

    private static final Logger logger = LoggerFactory.getLogger(JedisClusterSlaveUtils.class);

    public static Map<Integer, List<HostAndPort>> clusterSlotsSlaveNodes(JedisClusterWrapper jedisCluster) {
        List<JedisPool> jedisPoolList = jedisCluster.getJedisPoolList();
        for (JedisPool jedisPool : jedisPoolList) {
            Map<Integer, List<HostAndPort>> slotSlaveMap = new HashMap<>();
            try (Jedis jedis = jedisPool.getResource()) {
                List<Object> slots = jedis.clusterSlots();
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
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    private static HostAndPort generateHostAndPort(List<Object> hostInfos) {
        return new HostAndPort(redis.clients.util.SafeEncoder.encode((byte[]) hostInfos.get(0)), ((Long) hostInfos.get(1)).intValue());
    }

    private static List<Integer> getAssignedSlotArray(List<Object> slotInfo) {
        List<Integer> slotNums = new ArrayList<>();
        for (int slot = ((Long) slotInfo.get(0)).intValue(); slot <= ((Long) slotInfo.get(1)).intValue(); slot++) {
            slotNums.add(slot);
        }
        return slotNums;
    }
}
