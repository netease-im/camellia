package com.netease.nim.camellia.hot.key.sdk.netty;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.CamelliaDiscoveryFactory;
import com.netease.nim.camellia.core.discovery.ServerNode;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyPack;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyPackConsumer;
import com.netease.nim.camellia.hot.key.common.netty.pack.HeartbeatPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.HeartbeatRepPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.HotKeyCommand;
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
    private final ConcurrentHashMap<String, ConcurrentHashMap<ServerNode, HotKeyClientGroup>> clientGroupMap = new ConcurrentHashMap<>();
    //name -> discovery
    private final ConcurrentHashMap<String, CamelliaDiscovery> discoveryMap = new ConcurrentHashMap<>();
    //name -> addr-list
    private final ConcurrentHashMap<String, List<ServerNode>> addrMap = new ConcurrentHashMap<>();
    //lock-map
    private final ConcurrentHashMap<String, AtomicBoolean> lockMap = new ConcurrentHashMap<>();

    private final HotKeyPackBizClientHandler handler;
    private final HotKeyPackConsumer consumer;
    private final int connectNum;

    private static volatile HotKeyClientHub instance;
    private HotKeyClientHub() {
        this.connectNum = HotKeyConstants.Client.connectNum;
        this.handler = new HotKeyPackBizClientHandler(HotKeyConstants.Client.bizWorkThread, HotKeyConstants.Client.bizWorkQueueCapacity);
        this.consumer = new HotKeyPackConsumer(handler);
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("hot-key-client-heartbeat"))
                .scheduleAtFixedRate(this::scheduleHeartbeat, HotKeyConstants.Client.heartbeatIntervalSeconds, HotKeyConstants.Client.heartbeatIntervalSeconds, TimeUnit.SECONDS);
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("hot-key-client-reload"))
                .scheduleAtFixedRate(this::reload, HotKeyConstants.Client.reloadIntervalSeconds, HotKeyConstants.Client.reloadIntervalSeconds, TimeUnit.SECONDS);
        logger.info("HotKeyClientHub init success, workThread = {}, connectNum = {}, heartbeatIntervalSeconds = {}, reloadIntervalSeconds = {}",
                HotKeyConstants.Client.bizWorkThread, connectNum, HotKeyConstants.Client.heartbeatIntervalSeconds, HotKeyConstants.Client.reloadIntervalSeconds);
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

    /**
     * 注册一个业务回调
     * @param listener listener
     */
    public void registerListener(HotKeyClientListener listener) {
        handler.registerListener(listener);
    }

    /**
     * 注册一个服务器发现discovery
     * @param discoveryFactory discoveryFactory
     * @param serviceName service name
     */
    public void registerDiscovery(CamelliaDiscoveryFactory discoveryFactory, String serviceName) {
        CamelliaDiscovery discovery = discoveryFactory.getDiscovery(serviceName);
        CamelliaDiscovery old = discoveryMap.putIfAbsent(serviceName, discovery);
        if (old != null) {
            logger.info("HotKeyServerDiscovery duplicate register, will skip, name = {}", serviceName);
            return;
        }
        ConcurrentHashMap<ServerNode, HotKeyClientGroup> map = CamelliaMapUtils.computeIfAbsent(clientGroupMap, serviceName, k -> new ConcurrentHashMap<>());
        List<ServerNode> addrList = CamelliaMapUtils.computeIfAbsent(addrMap, serviceName, k -> new ArrayList<>());

        List<ServerNode> all = discovery.findAll();
        List<ServerNode> valid = new ArrayList<>();
        for (ServerNode addr : all) {
            HotKeyClientGroup clientGroup = new HotKeyClientGroup(addr, consumer, connectNum);
            if (clientGroup.isValid()) {
                map.put(addr, clientGroup);
                valid.add(addr);
                addrList.add(addr);
            }
        }
        Collections.sort(addrList);
        discovery.setCallback(new CamelliaDiscovery.Callback() {
            @Override
            public void add(ServerNode server) {
                HotKeyClientHub.this.add(serviceName, server);
            }
            @Override
            public void remove(ServerNode server) {
                HotKeyClientHub.this.remove(serviceName, server);
            }
        });
        logger.info("HotKeyServerDiscovery init {}, name = {} size = {}, all.list = {}, valid.list = {}", !valid.isEmpty(), serviceName, map.size(), all, valid);
    }

    /**
     * 选择一个HotKeyClient，基于hash规则，相同的key总是选择相同的client
     * @param name name
     * @param key key
     * @return HotKeyClient
     */
    public HotKeyClient selectClient(String name, String key) {
        HotKeyClient client = select0(name, key);
        if (client != null) {
            return client;
        }
        int retry1 = 3;
        while (retry1 -- > 0) {
            int retry2 = 3;
            while (retry2-- > 0) {
                client = select0(name, key);
                if (client != null) {
                    return client;
                }
            }
            reload(name);
        }
        return null;
    }

    private HotKeyClient select0(String name, String key) {
        try {
            List<ServerNode> addrs = addrMap.get(name);
            if (addrs == null || addrs.isEmpty()) {
                return null;
            }
            int index = Math.abs(key.hashCode()) % addrs.size();
            ServerNode addr = addrs.get(index);
            ConcurrentHashMap<ServerNode, HotKeyClientGroup> map = clientGroupMap.get(name);
            if (map == null || map.isEmpty()) {
                return null;
            }
            HotKeyClientGroup clientGroup = map.get(addr);
            if (clientGroup == null) {
                return null;
            }
            if (clientGroup.isValid()) {
                HotKeyClient client = clientGroup.select();
                if (client != null && client.isValid()) {
                    return client;
                }
            } else {
                remove(name, addr);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    //定时心跳
    private void scheduleHeartbeat() {
        if (scheduleLock.compareAndSet(false, true)) {
            try {
                ConcurrentHashMap<String, List<ServerNode>> map = new ConcurrentHashMap<>(addrMap);
                for (Map.Entry<String, List<ServerNode>> entry : map.entrySet()) {
                    String name = entry.getKey();
                    ConcurrentHashMap<ServerNode, HotKeyClientGroup> clientSubMap = clientGroupMap.get(name);
                    if (clientSubMap == null) continue;
                    List<ServerNode> list = new ArrayList<>(entry.getValue());
                    for (ServerNode addr : list) {
                        HotKeyClientGroup clientGroup = clientSubMap.get(addr);
                        clientGroup.addIfNotFull();
                        for (HotKeyClient client : clientGroup.getClientList()) {
                            CompletableFuture<HotKeyPack> future = client.sendPack(HotKeyPack.newPack(HotKeyCommand.HEARTBEAT, new HeartbeatPack()));
                            try {
                                HotKeyPack hotKeyPack = future.get(HotKeyConstants.Client.heartbeatTimeoutMillis, TimeUnit.MILLISECONDS);
                                if (!(hotKeyPack != null && hotKeyPack.getBody() instanceof HeartbeatRepPack)) {
                                    logger.warn("{} {} {} heartbeat error, will remove", name, addr, client.getId());
                                    clientGroup.remove(client);
                                }
                            } catch (Exception e) {
                                logger.warn("{} {} {} heartbeat error, will remove", name, addr, client.getId());
                                clientGroup.remove(client);
                            }
                        }
                        if (!clientGroup.isValid()) {
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

    //定时reload
    private void reload() {
        try {
            for (Map.Entry<String, CamelliaDiscovery> entry : discoveryMap.entrySet()) {
                reload(entry.getKey());
            }
        } catch (Exception e) {
            logger.error("reload error", e);
        }
    }

    //reload
    private void reload(String name) {
        CamelliaDiscovery discovery = discoveryMap.get(name);
        if (discovery == null) {
            logger.warn("skip reload for discovery not found, name = {}", name);
            return;
        }
        AtomicBoolean lock = CamelliaMapUtils.computeIfAbsent(lockMap, name, k -> new AtomicBoolean(false));
        if (lock.compareAndSet(false, true)) {
            try {
                Set<ServerNode> addrSet = new HashSet<>(discovery.findAll());
                if (addrSet.isEmpty()) {
                    return;
                }

                ConcurrentHashMap<ServerNode, HotKeyClientGroup> newClientGroupMap = new ConcurrentHashMap<>();
                ConcurrentHashMap<ServerNode, HotKeyClientGroup> oldClientGroupMap = CamelliaMapUtils.computeIfAbsent(clientGroupMap, name, k -> new ConcurrentHashMap<>());

                List<HotKeyClientGroup> toRemoveClientGroup = new ArrayList<>();
                List<ServerNode> toRemoveAddr = new ArrayList<>();
                for (Map.Entry<ServerNode, HotKeyClientGroup> entry : oldClientGroupMap.entrySet()) {
                    ServerNode addr = entry.getKey();
                    HotKeyClientGroup oldClientGroup = entry.getValue();
                    if (addrSet.contains(addr)) {
                        oldClientGroup.addIfNotFull();
                        if (oldClientGroup.isValid()) {
                            newClientGroupMap.put(addr, oldClientGroup);
                        }
                    } else {
                        toRemoveClientGroup.add(oldClientGroup);
                    }
                }
                for (ServerNode addr : addrSet) {
                    HotKeyClientGroup clientGroup = newClientGroupMap.get(addr);
                    if (clientGroup == null || !clientGroup.isValid()) {
                        HotKeyClientGroup newClientGroup = new HotKeyClientGroup(addr, consumer, connectNum);
                        if (newClientGroup.isValid()) {
                            newClientGroupMap.put(addr, newClientGroup);
                            continue;
                        }
                    }
                    toRemoveAddr.add(addr);
                }
                if (newClientGroupMap.isEmpty()) {
                    return;
                }
                toRemoveAddr.forEach(addrSet::remove);
                if (addrSet.isEmpty()) {
                    return;
                }
                List<ServerNode> newAddrs = new ArrayList<>(addrSet);
                Collections.sort(newAddrs);
                addrMap.put(name, newAddrs);
                clientGroupMap.put(name, newClientGroupMap);

                for (HotKeyClientGroup client : toRemoveClientGroup) {
                    client.stop();
                }
            } catch (Exception e) {
                logger.error("reload error, discovery.name = {}", name, e);
            } finally {
                lock.compareAndSet(true, false);
            }
        }
    }

    //增加一个节点
    private synchronized void add(String name, ServerNode addr) {
        try {
            boolean valid = false;
            ConcurrentHashMap<ServerNode, HotKeyClientGroup> map = CamelliaMapUtils.computeIfAbsent(clientGroupMap, name, k -> new ConcurrentHashMap<>());

            HotKeyClientGroup clientGroup = map.get(addr);
            if (clientGroup == null || !clientGroup.isValid()) {
                clientGroup = new HotKeyClientGroup(addr, consumer, connectNum);
            }
            if (clientGroup.isValid()) {
                map.put(addr, clientGroup);
                valid = true;
            }
            if (valid) {
                List<ServerNode> addrs = CamelliaMapUtils.computeIfAbsent(addrMap, name, k -> new ArrayList<>());
                HashSet<ServerNode> set = new HashSet<>(addrs);
                set.add(addr);
                List<ServerNode> newAddrs = new ArrayList<>(set);
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

    //移除一个节点
    private synchronized void remove(String name, ServerNode addr) {
        try {
            List<ServerNode> addrs = addrMap.get(name);
            HashSet<ServerNode> set = new HashSet<>(addrs);
            set.remove(addr);
            //如果已经是最后一个了，则不允许移除
            if (set.isEmpty()) {
                logger.error("HotKeyServerDiscovery = {}, last HotKeyServerAddr, skip remove = {}", name, addr);
                return;
            }
            List<ServerNode> newAddrs = new ArrayList<>(set);
            Collections.sort(newAddrs);
            addrMap.put(name, newAddrs);
            ConcurrentHashMap<ServerNode, HotKeyClientGroup> map = clientGroupMap.get(name);
            if (map != null) {
                HotKeyClientGroup clientGroup = map.remove(addr);
                if (clientGroup != null) {
                    clientGroup.stop();
                }
            }
            logger.info("HotKeyServerDiscovery = {}, remove addr = {}, newAddrs = {}", name, addr, newAddrs);
        } catch (Exception e) {
            logger.error("HotKeyServerDiscovery = {} remove addr = {} error", name, addr, e);
        }
    }
}
