package com.netease.nim.camellia.hot.key.server;

import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.nio.channels.Channel;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2023/5/9
 */
public class ClientConnectHub {

    private final ConcurrentHashMap<String, ChannelInfo> map = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> namespaceMap = new ConcurrentHashMap<>();

    private static final ClientConnectHub instance = new ClientConnectHub();
    private ClientConnectHub() {
    }
    public static ClientConnectHub getInstance() {
        return instance;
    }

    public ChannelInfo get(String consid) {
        return map.get(consid);
    }

    public void add(ChannelInfo channelInfo) {
        map.put(channelInfo.getConsid(), channelInfo);
    }

    public void remove(ChannelInfo channelInfo) {
        map.remove(channelInfo.getConsid());
        for (String namespace : channelInfo.namespaceSet()) {
            ConcurrentHashMap<String, Boolean> subMap = namespaceMap.get(namespace);
            if (subMap != null) {
                subMap.remove(channelInfo.getConsid());
            }
        }
    }

    public void updateNamespace(String namespace, ChannelInfo channelInfo) {
        ConcurrentHashMap<String, Boolean> subMap = CamelliaMapUtils.computeIfAbsent(namespaceMap, namespace, k -> new ConcurrentHashMap<>());
        subMap.put(channelInfo.getConsid(), true);
    }

    public ConcurrentHashMap<String, ChannelInfo> getMap() {
        return map;
    }

    public ConcurrentHashMap<String, Boolean> getMap(String namespace) {
        return namespaceMap.get(namespace);
    }
}
