package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2019/11/7.
 */
public class ChannelMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ChannelMonitor.class);

    private static final ConcurrentHashMap<String, ChannelInfo> map = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, ChannelInfo>> bidbgroupMap = new ConcurrentHashMap<>();

    public static void init(ChannelInfo channelInfo) {
        ExecutorUtils.submitToSingleThreadExecutor(() -> {
            try {
                map.put(channelInfo.getConsid(), channelInfo);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    public static void remove(ChannelInfo channelInfo) {
        ExecutorUtils.submitToSingleThreadExecutor(() -> {
            try {
                map.remove(channelInfo.getConsid(), channelInfo);
                Long bid = channelInfo.getBid();
                String bgroup = channelInfo.getBgroup();
                if (bid != null && bgroup != null) {
                    String key = bid + "|" + bgroup;
                    ConcurrentHashMap<String, ChannelInfo> subMap = bidbgroupMap.get(key);
                    if (subMap != null) {
                        subMap.remove(channelInfo.getConsid());
                        if (subMap.isEmpty()) {
                            bidbgroupMap.remove(key);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    public static Map<String, ChannelInfo> getChannelMap() {
        return new HashMap<>(map);
    }

    public static Set<ChannelInfo> getChannelMap(long bid, String bgroup) {
        if (bid == -1) {
            return new HashSet<>(map.values());
        }
        ConcurrentHashMap<String, ChannelInfo> map = bidbgroupMap.get(bid + "|" + bgroup);
        if (map == null) return new HashSet<>();
        return new HashSet<>(map.values());
    }

    public static int connect() {
        return map.size();
    }

    public static int bidBgroupConnect(long bid, String bgroup) {
        String key = bid + "|" + bgroup;
        ConcurrentHashMap<String, ChannelInfo> subMap = bidbgroupMap.get(key);
        if (subMap == null) return 0;
        return subMap.size();
    }

    public static void initBidBgroup(long bid, String bgroup, ChannelInfo channelInfo) {
        ExecutorUtils.submitToSingleThreadExecutor(() -> {
            try {
                String key = bid + "|" + bgroup;
                ConcurrentHashMap<String, ChannelInfo> subMap = bidbgroupMap.get(key);
                if (subMap == null) {
                    subMap = new ConcurrentHashMap<>();
                    bidbgroupMap.put(key, subMap);
                }
                subMap.put(channelInfo.getConsid(), channelInfo);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }
}
