package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.BidBgroupConnectStats;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监控channel的情况
 * <p>Monitor the status of the channel
 * <p>
 * Created by caojiajun on 2019/11/7.
 */
public class ChannelMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ChannelMonitor.class);

    /**
     * The channel map aims to record all {@link ChannelInfo} objects.
     */
    private static final ConcurrentHashMap<String, ChannelInfo> map = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, ChannelInfo> idMap = new ConcurrentHashMap<>();
    /**
     * 多租户{@link ChannelInfo} map
     * <p>
     * multi-tenant {@link ChannelInfo} map
     */
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, ChannelInfo>> bidbgroupMap = new ConcurrentHashMap<>();

    /**
     * Put channelInfo into {@link ChannelMonitor#map}
     * @param channelInfo channelInfo
     */
    public static void init(ChannelInfo channelInfo) {
        ExecutorUtils.submitToSingleThreadExecutor(() -> {
            try {
                map.put(channelInfo.getConsid(), channelInfo);
                idMap.put(channelInfo.getId(), channelInfo);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    /**
     * Remove channelInfo from {@link ChannelMonitor#map}. If multi-tenant is used, the channelInfo will be removed from {@link ChannelMonitor#bidbgroupMap} as well.
     * @param channelInfo the channelInfo object
     */
    public static void remove(ChannelInfo channelInfo) {
        ExecutorUtils.submitToSingleThreadExecutor(() -> {
            try {
                map.remove(channelInfo.getConsid());
                idMap.remove(channelInfo.getId());
                Long bid = channelInfo.getBid();
                String bgroup = channelInfo.getBgroup();
                if (bid != null && bgroup != null) {
                    String key = Utils.getCacheKey(bid, bgroup);
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

    public static ChannelInfo getChannel(String consid) {
        return map.get(consid);
    }

    public static ChannelInfo getChannelById(long id) {
        return idMap.get(id);
    }

    public static Set<ChannelInfo> getChannelMap(long bid, String bgroup) {
        if (bid == -1) {
            return new HashSet<>(map.values());
        }
        ConcurrentHashMap<String, ChannelInfo> map = bidbgroupMap.get(bid + "|" + bgroup);
        if (map == null) return new HashSet<>();
        return new HashSet<>(map.values());
    }

    /**
     * @return the channelInfo number in {@link ChannelMonitor#map}
     */
    public static int connect() {
        return map.size();
    }

    public static List<BidBgroupConnectStats> bidBgroupConnect() {
        List<BidBgroupConnectStats> list = new ArrayList<>();
        for (Map.Entry<String, ConcurrentHashMap<String, ChannelInfo>> entry : bidbgroupMap.entrySet()) {
            BidBgroupConnectStats stats = new BidBgroupConnectStats();
            String[] split = entry.getKey().split("\\|");
            stats.setBid(Long.parseLong(split[0]));
            stats.setBgroup(split[1]);
            stats.setConnect(entry.getValue().size());
            list.add(stats);
        }
        return list;
    }

    public static int bidBgroupConnect(long bid, String bgroup) {
        String key = Utils.getCacheKey(bid, bgroup);
        ConcurrentHashMap<String, ChannelInfo> subMap = bidbgroupMap.get(key);
        if (subMap == null) return 0;
        return subMap.size();
    }

    /**
     * Put the channelInfo object into {@link ChannelMonitor#bidbgroupMap}. This mthod is thread-safe,
     * @param bid bid
     * @param bgroup bgroup
     * @param channelInfo channelInfo
     */
    public static void initBidBgroup(long bid, String bgroup, ChannelInfo channelInfo) {
        ExecutorUtils.submitToSingleThreadExecutor(() -> {
            try {
                String key = Utils.getCacheKey(bid, bgroup);
                ConcurrentHashMap<String, ChannelInfo> subMap = CamelliaMapUtils.computeIfAbsent(bidbgroupMap, key, k -> new ConcurrentHashMap<>());
                subMap.put(channelInfo.getConsid(), channelInfo);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }
}
