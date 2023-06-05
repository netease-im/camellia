package com.netease.nim.camellia.redis.proxy.hotkey.common;

import com.netease.nim.camellia.hot.key.sdk.discovery.HotKeyServerDiscovery;
import com.netease.nim.camellia.hot.key.sdk.discovery.LocalConfHotKeyServerDiscovery;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerAddr;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

import java.util.ArrayList;
import java.util.List;

public class ProxyLocalHotKeyServerDiscoveryFactory implements ProxyHotKeyServerDiscoveryFactory {

    @Override
    public HotKeyServerDiscovery getDiscovery() {
        String address = ProxyDynamicConf.getString("hot.key.server.local.addr", "127.0.0.1:7070");
        String name = ProxyDynamicConf.getString("hot.key.server.local.name", "local");
        return new LocalConfHotKeyServerDiscovery(name, getAddressList(address));
    }

    private static List<HotKeyServerAddr> getAddressList(String address) {
        List<HotKeyServerAddr> addrList = new ArrayList<>();
        String[] strings = address.trim().split(",");
        for (String str : strings) {
            int index = str.lastIndexOf(":");
            String host = str.substring(0, index);
            String port = str.substring(index + 1);
            addrList.add(new HotKeyServerAddr(host, Integer.parseInt(port)));
        }
        return addrList;
    }
}
