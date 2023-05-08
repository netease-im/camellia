package com.netease.nim.camellia.hot.key.sdk.netty;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyPackConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyClientHub {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyClientHub.class);

    private final ConcurrentHashMap<String, ConcurrentHashMap<HotKeyServerAddr, HotKeyClient>> clientMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, HotKeyServerDiscovery> discoveryMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<HotKeyServerAddr>> addrMap = new ConcurrentHashMap<>();

    private HotKeyPackConsumer consumer;

    private static final HotKeyClientHub instance = new HotKeyClientHub();
    private HotKeyClientHub() {
    }
    public static HotKeyClientHub getInstance() {
        return instance;
    }


    /**
     * 注册一个服务器发现discovery
     * @param discovery discovery
     * @return 成功/失败
     */
    public boolean register(HotKeyServerDiscovery discovery) {
        String name = discovery.getName();
        HotKeyServerDiscovery old = discoveryMap.putIfAbsent(name, discovery);
        if (old != null) {
            logger.error("HotKeyServerDiscovery = {} duplicate register, will skip", name);
            return false;
        }
        ConcurrentHashMap<HotKeyServerAddr, HotKeyClient> map = clientMap.get(name);
        List<HotKeyServerAddr> addrList = addrMap.get(name);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            clientMap.put(name, map);
        }
        if (addrList == null) {
            addrList = new ArrayList<>();
            addrMap.put(name, addrList);
        }
        List<HotKeyServerAddr> all = discovery.findAll();
        List<HotKeyServerAddr> valid = new ArrayList<>();
        for (HotKeyServerAddr addr : all) {
            HotKeyClient client = new HotKeyClient(addr, consumer);
            if (client.isValid()) {
                map.put(addr, client);
                valid.add(addr);
                addrList.add(addr);
            }
        }
        Collections.sort(addrList);
        discovery.setCallback(new CamelliaDiscovery.Callback<HotKeyServerAddr>() {
            @Override
            public void add(HotKeyServerAddr server) {
                HotKeyClientHub.this.add(discovery, server);
            }
            @Override
            public void remove(HotKeyServerAddr server) {
                HotKeyClientHub.this.remove(discovery, server);
            }
        });
        logger.info("HotKeyServerDiscovery = {} init {}, size = {}, all.list = {}, valid.list = {}", name, !valid.isEmpty(), map.size(), all, valid);
        return !valid.isEmpty();
    }

    /**
     * 选择一个HotKeyClient，基于hash规则，相同的key总是选择相同的client
     * @param discovery discovery
     * @param key key
     * @return HotKeyClient
     */
    public HotKeyClient select(HotKeyServerDiscovery discovery, String key) {
        String name = discovery.getName();
        int retry = 3;
        while (retry -- > 0) {
            HotKeyClient client = select0(name, key);
            if (client != null) {
                return client;
            }
        }
        return null;
    }

    private synchronized void add(HotKeyServerDiscovery discovery, HotKeyServerAddr addr) {
        List<HotKeyServerAddr> addrs = addrMap.get(discovery.getName());
        HashSet<HotKeyServerAddr> set = new HashSet<>(addrs);
        set.add(addr);
        List<HotKeyServerAddr> newAddrs = new ArrayList<>(set);
        Collections.sort(newAddrs);
        addrMap.put(discovery.getName(), newAddrs);
        logger.info("HotKeyServerDiscovery = {}, add addr = {}, newAddrs = {}", discovery.getName(), addr, newAddrs);
    }

    private synchronized void remove(HotKeyServerDiscovery discovery, HotKeyServerAddr addr) {
        List<HotKeyServerAddr> addrs = addrMap.get(discovery.getName());
        HashSet<HotKeyServerAddr> set = new HashSet<>(addrs);
        set.remove(addr);
        if (set.isEmpty()) {
            logger.error("HotKeyServerDiscovery = {}, last HotKeyServerAddr, skip remove = {}", discovery.getName(), addr);
            return;
        }
        List<HotKeyServerAddr> newAddrs = new ArrayList<>(set);
        Collections.sort(newAddrs);
        addrMap.put(discovery.getName(), newAddrs);
        logger.info("HotKeyServerDiscovery = {}, remove addr = {}, newAddrs = {}", discovery.getName(), addr, newAddrs);
    }

    private HotKeyClient select0(String name, String key) {
        try {
            List<HotKeyServerAddr> addrs = addrMap.get(name);
            if (addrs == null || addrs.isEmpty()) {
                return null;
            }
            int index = Math.abs(key.hashCode()) % addrs.size();
            HotKeyServerAddr addr = addrs.get(index);
            ConcurrentHashMap<HotKeyServerAddr, HotKeyClient> map = clientMap.get(name);
            if (map == null || map.isEmpty()) {
                return null;
            }
            HotKeyClient client = map.get(addr);
            if (client.isValid()) {
                return client;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
