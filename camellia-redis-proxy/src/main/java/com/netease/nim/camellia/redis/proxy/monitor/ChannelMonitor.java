package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2019/11/7.
 */
public class ChannelMonitor {

    private static ConcurrentHashMap<String, ChannelInfo> map = new ConcurrentHashMap<>();

    public static void init(ChannelInfo channelInfo) {
        map.put(channelInfo.getConsid(), channelInfo);
    }

    public static void remove(ChannelInfo channelInfo) {
        map.remove(channelInfo.getConsid());
    }

    public static Map<String, ChannelInfo> getChannelMap() {
        return Collections.unmodifiableMap(map);
    }
}
