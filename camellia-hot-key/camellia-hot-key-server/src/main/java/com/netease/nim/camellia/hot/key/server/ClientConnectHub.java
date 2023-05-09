package com.netease.nim.camellia.hot.key.server;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2023/5/9
 */
public class ClientConnectHub {

    private final ConcurrentHashMap<String, ChannelInfo> map = new ConcurrentHashMap<>();

    private static final ClientConnectHub instance = new ClientConnectHub();
    private ClientConnectHub() {
    }
    public static ClientConnectHub getInstance() {
        return instance;
    }

    public void add(ChannelInfo channelInfo) {
        map.put(channelInfo.getConsid(), channelInfo);
    }

    public void remove(ChannelInfo channelInfo) {
        map.remove(channelInfo.getConsid());
    }

    public ConcurrentHashMap<String, ChannelInfo> getMap() {
        return map;
    }
}
