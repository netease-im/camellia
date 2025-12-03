package com.netease.nim.camellia.naming.zk;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.naming.core.CamelliaNamingException;
import com.netease.nim.camellia.naming.core.ICamelliaNamingCallback;
import com.netease.nim.camellia.naming.core.ICamelliaNamingService;
import com.netease.nim.camellia.naming.core.InstanceInfo;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2025/11/18
 */
public class CamelliaZkNamingService implements ICamelliaNamingService {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaZkNamingService.class);

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, new CamelliaThreadFactory("camellia-naming-zk-scheduler"));

    private final CuratorFramework client;
    private final String basePath;
    private final String serviceName;
    private final String registerPath;
    private final InstanceInfo instanceInfo;
    private boolean registerOk;

    private final AtomicBoolean registering = new AtomicBoolean(false);

    private final Map<String, PathChildrenCache> zkWatchMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, ICamelliaNamingCallback>> callbackMap = new ConcurrentHashMap<>();
    private final Map<String, Set<InstanceInfo>> instanceMap = new ConcurrentHashMap<>();
    private final Map<String, Long> timeMap = new ConcurrentHashMap<>();

    public CamelliaZkNamingService(CuratorFramework client, String basePath, String serviceName, InstanceInfo instanceInfo) {
        this.client = client;
        this.basePath = basePath;
        this.serviceName = serviceName;
        this.instanceInfo = instanceInfo;
        this.registerPath = basePath + "/" + serviceName + "/" + UUID.randomUUID().toString().replace("-", "");

        client.getConnectionStateListenable().addListener((curatorFramework, connectionState) -> {
            if (connectionState == ConnectionState.LOST) {
                logger.warn("zk connectionState LOST");
                while (true) {
                    try {
                        if (curatorFramework.getZookeeperClient().blockUntilConnectedOrTimedOut()) {
                            if (registerOk) {
                                registerOk = false;
                                register();
                            }
                            break;
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });

        int initialDelay = ThreadLocalRandom.current().nextInt(60);
        scheduler.scheduleAtFixedRate(this::reloadAll, initialDelay, 60, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (registerOk) {
                    deregister();
                }
            } catch (Exception e) {
                logger.error("deregister error, serviceName = {}, instanceInfo = {}", serviceName, JSONObject.toJSONString(instanceInfo), e);
            }
        }));
    }

    @Override
    public InstanceInfo getInstanceInfo() {
        return instanceInfo;
    }

    @Override
    public List<InstanceInfo> getInstanceInfoList(String serviceName) {
        try {
            Set<InstanceInfo> list = instanceMap.get(serviceName);
            if (list != null) {
                Long lastRefreshTime = timeMap.get(serviceName);
                if (lastRefreshTime != null && System.currentTimeMillis() - lastRefreshTime < 1000) {
                    return new ArrayList<>(list);
                }
            }
            return new ArrayList<>(reload(serviceName));
        } catch (Exception e) {
            logger.error("getInstanceList error, serviceName = {}", serviceName, e);
            throw new CamelliaNamingException(e);
        }
    }

    @Override
    public String subscribe(String serviceName, ICamelliaNamingCallback callback) {
        try {
            //
            reload(serviceName);
            //
            initZkWatch(serviceName);
            //
            Map<String, ICamelliaNamingCallback> subMap = CamelliaMapUtils.computeIfAbsent(callbackMap, serviceName, k -> new ConcurrentHashMap<>());
            String id = UUID.randomUUID().toString().replace("-", "");
            subMap.put(id, callback);
            return id;
        } catch (Exception e) {
            logger.error("subscribe error, serviceName = {}", serviceName, e);
            throw new CamelliaNamingException(e);
        }
    }

    @Override
    public void unsubscribe(String serviceName, String id) {
        try {
            Map<String, ICamelliaNamingCallback> subMap = callbackMap.get(serviceName);
            if (subMap == null) {
                return;
            }
            subMap.remove(id);
        } catch (Exception e) {
            logger.error("unsubscribe error, serviceName = {}", serviceName, e);
            throw new CamelliaNamingException(e);
        }
    }

    @Override
    public void register() {
        if (registering.compareAndSet(false, true)) {
            try {
                if (registerOk) {
                    try {
                        byte[] data = client.getData().forPath(registerPath);
                        InstanceInfo instance = JSONObject.parseObject(new String(data, StandardCharsets.UTF_8), InstanceInfo.class);
                        if (Objects.equals(instance, instanceInfo)) {
                            return;
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                while (true) {
                    try {
                        byte[] data = JSONObject.toJSONString(instanceInfo).getBytes(StandardCharsets.UTF_8);
                        client.create().creatingParentContainersIfNeeded()
                                .withMode(CreateMode.EPHEMERAL).forPath(registerPath, data);
                        registerOk = true;
                        logger.info("register to zk, serviceName = {}, instanceInfo = {}", serviceName, JSONObject.toJSONString(instanceInfo));
                        break;
                    } catch (KeeperException.NodeExistsException e) {
                        try {
                            byte[] data = client.getData().forPath(registerPath);
                            InstanceInfo instance = JSONObject.parseObject(new String(data, StandardCharsets.UTF_8), InstanceInfo.class);
                            if (instanceInfo != null) {
                                if (Objects.equals(instanceInfo, instance)) {
                                    logger.info("has register to zk, serviceName = {}, instanceInfo = {}", serviceName, JSONObject.toJSONString(instanceInfo));
                                    break;
                                }
                            }
                            client.delete().forPath(registerPath);
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                    } catch (Exception e) {
                        logger.error("register error, serviceName = {}, instanceInfo = {}", serviceName, JSONObject.toJSON(instanceInfo), e);
                        throw new CamelliaNamingException(e);
                    }
                }
            } finally {
                registering.compareAndSet(true, false);
            }
        } else {
            throw new CamelliaNamingException("concurrent register/deregister");
        }
    }

    @Override
    public void deregister() {
        if (registering.compareAndSet(false, true)) {
            try {
                if (!registerOk) return;
                client.delete().forPath(registerPath);
                registerOk = false;
                logger.info("deregister to zk, serviceName = {}, instanceInfo = {}", serviceName, JSONObject.toJSONString(instanceInfo));
            } catch (KeeperException.NoNodeException e) {
                registerOk = false;
                logger.info("not register to zk, skip deregister, serviceName = {}, instanceInfo = {}", serviceName, JSONObject.toJSONString(instanceInfo));
            } catch (Exception e) {
                logger.error("deregister error, serviceName = {}, instanceInfo = {}", serviceName, JSONObject.toJSON(instanceInfo), e);
                throw new CamelliaNamingException(e);
            } finally {
                registering.compareAndSet(true, false);
            }
        } else {
            throw new CamelliaNamingException("concurrent register/deregister");
        }
    }

    private void initZkWatch(String serviceName) {
        PathChildrenCache cache = zkWatchMap.get(serviceName);
        if (cache != null) {
            return;
        }
        synchronized (zkWatchMap) {
            cache = zkWatchMap.get(serviceName);
            if (cache != null) {
                return;
            }
            cache = new PathChildrenCache(this.client, basePath + "/" + serviceName, true);
            cache.getListenable().addListener((curatorClient, event) -> {
                try {
                    if (event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED) {
                        ChildData childData = event.getData();
                        String path = childData.getPath();
                        byte[] data = childData.getData();
                        if (data == null) {
                            data = curatorClient.getData().forPath(path);
                        }
                        if (data == null) {
                            logger.warn("child_added, but data is null, serviceName = {}, path = {}", serviceName, path);
                        } else {
                            InstanceInfo instanceInfo = JSONObject.parseObject(new String(data, StandardCharsets.UTF_8), InstanceInfo.class);
                            synchronized (instanceMap) {
                                Set<InstanceInfo> set = CamelliaMapUtils.computeIfAbsent(instanceMap, serviceName, k -> new HashSet<>());
                                set.add(instanceInfo);
                            }
                            if (logger.isDebugEnabled()) {
                                logger.debug("instance info added, instance = {}", JSONObject.toJSON(instanceInfo));
                            }
                            Map<String, ICamelliaNamingCallback> subMap = callbackMap.get(serviceName);
                            if (subMap != null) {
                                for (Map.Entry<String, ICamelliaNamingCallback> entry : subMap.entrySet()) {
                                    try {
                                        entry.getValue().add(Collections.singletonList(instanceInfo));
                                    } catch (Exception e) {
                                        logger.error("add callback error, serviceName = {}", serviceName, e);
                                    }
                                }
                            }
                        }
                    } else if (event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
                        ChildData childData = event.getData();
                        String path = childData.getPath();
                        byte[] data = childData.getData();
                        if (data == null) {
                            data = curatorClient.getData().forPath(path);
                        }
                        if (data == null) {
                            logger.warn("child_removed, but data is null, serviceName = {}, path = {}", serviceName, path);
                        } else {
                            InstanceInfo instanceInfo = JSONObject.parseObject(new String(data, StandardCharsets.UTF_8), InstanceInfo.class);
                            synchronized (instanceMap) {
                                Set<InstanceInfo> set = CamelliaMapUtils.computeIfAbsent(instanceMap, serviceName, k -> new HashSet<>());
                                set.remove(instanceInfo);
                            }
                            if (logger.isDebugEnabled()) {
                                logger.debug("instance info removed, instance = {}", JSONObject.toJSON(instanceInfo));
                            }
                            Map<String, ICamelliaNamingCallback> subMap = callbackMap.get(serviceName);
                            if (subMap != null) {
                                for (Map.Entry<String, ICamelliaNamingCallback> entry : subMap.entrySet()) {
                                    try {
                                        entry.getValue().remove(Collections.singletonList(instanceInfo));
                                    } catch (Exception e) {
                                        logger.error("remove callback error, serviceName = {}", serviceName, e);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("PathChildrenCache listener error, serviceName = {}", serviceName, e);
                }
            });
            try {
                cache.start();
            } catch (Exception e) {
                throw new CamelliaNamingException(e);
            }
            zkWatchMap.put(serviceName, cache);
        }
    }

    private void reloadAll() {
        try {
            Set<String> serviceNameSet = new HashSet<>(instanceMap.keySet());
            for (String serviceName : serviceNameSet) {
                try {
                    reloadAndNotify(serviceName);
                } catch (Exception e) {
                    logger.error("reload error, serviceName = {}", serviceName, e);
                }
            }
        } catch (Exception e) {
            logger.error("reloadAll error", e);
        }
    }

    private void reloadAndNotify(String serviceName) {
        Set<InstanceInfo> oldSet = new HashSet<>(instanceMap.get(serviceName));
        Set<InstanceInfo> newSet = reload(serviceName);
        Set<InstanceInfo> added = new HashSet<>(newSet);
        added.removeAll(oldSet);
        Set<InstanceInfo> removed = new HashSet<>(oldSet);
        removed.removeAll(newSet);

        if (added.isEmpty() && removed.isEmpty()) {
            return;
        }

        Map<String, ICamelliaNamingCallback> callbackItemMap = callbackMap.get(serviceName);
        if (callbackItemMap != null) {
            for (Map.Entry<String, ICamelliaNamingCallback> entry : callbackItemMap.entrySet()) {
                ICamelliaNamingCallback callback = entry.getValue();
                if (!added.isEmpty()) {
                    try {
                        List<InstanceInfo> list = new ArrayList<>(added);
                        Collections.shuffle(list);
                        callback.add(list);
                    } catch (Exception e) {
                        logger.error("add callback error, serviceName = {}", serviceName, e);
                    }
                }
                if (!removed.isEmpty()) {
                    try {
                        List<InstanceInfo> list = new ArrayList<>(removed);
                        Collections.shuffle(list);
                        callback.remove(list);
                    } catch (Exception e) {
                        logger.error("remove callback error, serviceName = {}", serviceName, e);
                    }
                }
            }
        }
    }

    private Set<InstanceInfo> reload(String serviceName) {
        try {
            String path = basePath + "/" + serviceName;
            List<String> strings = client.getChildren().forPath(path);
            Set<InstanceInfo> set = new HashSet<>();
            if (strings != null) {
                for (String id : strings) {
                    try {
                        byte[] data = client.getData().forPath(path + "/" + id);
                        InstanceInfo instanceInfo = JSONObject.parseObject(new String(data, StandardCharsets.UTF_8), InstanceInfo.class);
                        if (instanceInfo != null) {
                            set.add(instanceInfo);
                        }
                    } catch (Exception e) {
                        logger.error("zk data parse error", e);
                    }
                }
            }
            instanceMap.put(serviceName, new HashSet<>(set));
            timeMap.put(serviceName, System.currentTimeMillis());
            return set;
        } catch (Exception e) {
            logger.error("reload error, serviceName = {}", serviceName, e);
            throw new CamelliaNamingException(e);
        }
    }

}
