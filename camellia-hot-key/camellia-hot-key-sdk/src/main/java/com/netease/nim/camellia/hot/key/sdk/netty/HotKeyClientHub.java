package com.netease.nim.camellia.hot.key.sdk.netty;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyPack;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyPackConsumer;
import com.netease.nim.camellia.hot.key.common.netty.pack.HeartbeatPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.HeartbeatRepPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.HotKeyCommand;
import com.netease.nim.camellia.hot.key.sdk.discovery.HotKeyServerDiscovery;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyClientHub {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyClientHub.class);

    private final AtomicBoolean scheduleLock = new AtomicBoolean(false);
    //name -> addr -> client
    private final ConcurrentHashMap<String, ConcurrentHashMap<HotKeyServerAddr, HotKeyClient>> clientMap = new ConcurrentHashMap<>();
    //name -> discovery
    private final ConcurrentHashMap<String, HotKeyServerDiscovery> discoveryMap = new ConcurrentHashMap<>();
    //name -> addr-list
    private final ConcurrentHashMap<String, List<HotKeyServerAddr>> addrMap = new ConcurrentHashMap<>();

    private final HotKeyPackBizClientHandler handler;
    private final HotKeyPackConsumer consumer;

    private static volatile HotKeyClientHub instance;
    private HotKeyClientHub() {
        this.handler = new HotKeyPackBizClientHandler(HotKeyConstants.Client.bizWorkThread, HotKeyConstants.Client.bizWorkQueueCapacity);
        this.consumer = new HotKeyPackConsumer(handler);
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("hot-key-client-heartbeat"))
                .scheduleAtFixedRate(this::scheduleHeartbeat, HotKeyConstants.Client.heartbeatIntervalSeconds, HotKeyConstants.Client.heartbeatIntervalSeconds, TimeUnit.SECONDS);
        logger.info("HotKeyClientHub init success, workThread = {}, heartbeatIntervalSeconds = {}",
                HotKeyConstants.Client.bizWorkThread, HotKeyConstants.Client.heartbeatIntervalSeconds);
    }
    public static HotKeyClientHub getInstance() {
        if (instance == null) {
            synchronized (HotKeyClientHub.class) {
                if (instance == null) {
                    instance = new HotKeyClientHub();
                }
            }
        }
        return instance;
    }

    private void scheduleHeartbeat() {
        if (scheduleLock.compareAndSet(false, true)) {
            try {
                ConcurrentHashMap<String, List<HotKeyServerAddr>> map = new ConcurrentHashMap<>(addrMap);
                for (Map.Entry<String, List<HotKeyServerAddr>> entry : map.entrySet()) {
                    String name = entry.getKey();
                    ConcurrentHashMap<HotKeyServerAddr, HotKeyClient> clientSubMap = clientMap.get(name);
                    if (clientSubMap == null) continue;
                    List<HotKeyServerAddr> list = new ArrayList<>(entry.getValue());
                    for (HotKeyServerAddr addr : list) {
                        HotKeyClient client = clientSubMap.get(addr);
                        CompletableFuture<HotKeyPack> future = client.sendPack(HotKeyPack.newPack(HotKeyCommand.HEARTBEAT, new HeartbeatPack()));
                        try {
                            HotKeyPack hotKeyPack = future.get(HotKeyConstants.Client.heartbeatTimeoutMillis, TimeUnit.MILLISECONDS);
                            if (!(hotKeyPack != null && hotKeyPack.getBody() instanceof HeartbeatRepPack)) {
                                logger.warn("{} {} heartbeat error, will remove", name, addr);
                                remove(name, addr);
                            }
                        } catch (Exception e) {
                            logger.warn("{} {} heartbeat error, will remove", name, addr);
                            remove(name, addr);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("scheduleHeartbeat error", e);
            } finally {
                scheduleLock.compareAndSet(true, false);
            }
        }
    }

    /**
     * 注册一个业务回调
     * @param listener listener
     */
    public void registerListener(HotKeyClientListener listener) {
        handler.registerListener(listener);
    }

    /**
     * 注册一个服务器发现discovery
     * @param discovery discovery
     */
    public void registerDiscovery(HotKeyServerDiscovery discovery) {
        String name = discovery.getName();
        HotKeyServerDiscovery old = discoveryMap.putIfAbsent(name, discovery);
        if (old != null) {
            logger.error("HotKeyServerDiscovery = {} duplicate register, will skip", name);
            return;
        }
        ConcurrentHashMap<HotKeyServerAddr, HotKeyClient> map = CamelliaMapUtils.computeIfAbsent(clientMap, name, k -> new ConcurrentHashMap<>());
        List<HotKeyServerAddr> addrList = CamelliaMapUtils.computeIfAbsent(addrMap, name, k -> new ArrayList<>());

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
                HotKeyClientHub.this.add(discovery.getName(), server);
            }
            @Override
            public void remove(HotKeyServerAddr server) {
                HotKeyClientHub.this.remove(discovery.getName(), server);
            }
        });
        logger.info("HotKeyServerDiscovery = {} init {}, size = {}, all.list = {}, valid.list = {}", name, !valid.isEmpty(), map.size(), all, valid);
    }

    /**
     * 选择一个HotKeyClient，基于hash规则，相同的key总是选择相同的client
     * @param discovery discovery
     * @param key key
     * @return HotKeyClient
     */
    public HotKeyClient selectClient(HotKeyServerDiscovery discovery, String key) {
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

    private synchronized void add(String name, HotKeyServerAddr addr) {
        try {
            boolean valid = false;
            ConcurrentHashMap<HotKeyServerAddr, HotKeyClient> map = CamelliaMapUtils.computeIfAbsent(clientMap, name, k -> new ConcurrentHashMap<>());

            HotKeyClient client = map.get(addr);
            if (client == null || !client.isValid()) {
                client = new HotKeyClient(addr, consumer);
            }
            if (client.isValid()) {
                map.put(addr, client);
                valid = true;
            }
            if (valid) {
                List<HotKeyServerAddr> addrs = CamelliaMapUtils.computeIfAbsent(addrMap, name, k -> new ArrayList<>());
                HashSet<HotKeyServerAddr> set = new HashSet<>(addrs);
                set.add(addr);
                List<HotKeyServerAddr> newAddrs = new ArrayList<>(set);
                Collections.sort(newAddrs);
                addrMap.put(name, newAddrs);
                logger.info("HotKeyServerDiscovery = {}, add addr = {}, newAddrs = {}", name, addr, newAddrs);
            } else {
                logger.warn("HotKeyServerDiscovery = {}, try add addr = {}, but failed", name, addr);
            }
        } catch (Exception e) {
            logger.error("HotKeyServerDiscovery = {} add addr = {} error", name, addr, e);
        }
    }

    private synchronized void remove(String name, HotKeyServerAddr addr) {
        try {
            List<HotKeyServerAddr> addrs = addrMap.get(name);
            HashSet<HotKeyServerAddr> set = new HashSet<>(addrs);
            set.remove(addr);
            if (set.isEmpty()) {
                logger.error("HotKeyServerDiscovery = {}, last HotKeyServerAddr, skip remove = {}", name, addr);
                return;
            }
            List<HotKeyServerAddr> newAddrs = new ArrayList<>(set);
            Collections.sort(newAddrs);
            addrMap.put(name, newAddrs);
            logger.info("HotKeyServerDiscovery = {}, remove addr = {}, newAddrs = {}", name, addr, newAddrs);
        } catch (Exception e) {
            logger.error("HotKeyServerDiscovery = {} remove addr = {} error", name, addr, e);
        }
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
            } else {
                remove(name, addr);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
