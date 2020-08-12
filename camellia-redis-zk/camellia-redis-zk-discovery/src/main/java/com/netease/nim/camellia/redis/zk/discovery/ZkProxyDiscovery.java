package com.netease.nim.camellia.redis.zk.discovery;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.proxy.Proxy;
import com.netease.nim.camellia.redis.proxy.ProxyDiscovery;
import com.netease.nim.camellia.redis.zk.common.InstanceInfo;
import com.netease.nim.camellia.redis.zk.common.InstanceInfoSerializeUtil;
import com.netease.nim.camellia.redis.zk.common.ZkConstants;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 *
 * Created by caojiajun on 2020/8/10
 */
public class ZkProxyDiscovery extends ProxyDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(ZkProxyDiscovery.class);

    private final CuratorFramework client;
    private final String path;
    private ConcurrentHashMap<String, InstanceInfo> map = new ConcurrentHashMap<>();

    public ZkProxyDiscovery(String zkUrl, String applicationName) {
        this(zkUrl, ZkConstants.basePath, applicationName);
    }

    public ZkProxyDiscovery(String zkUrl, String basePath, String applicationName) {
        this(ZkClientFactory.DEFAULT.getClient(zkUrl), basePath, applicationName, 600);
    }

    public ZkProxyDiscovery(ZkClientFactory factory, String zkUrl, String basePath, String applicationName, long reloadIntervalSeconds) {
        this(factory.getClient(zkUrl), basePath, applicationName, reloadIntervalSeconds);
    }

    public ZkProxyDiscovery(CuratorFramework client, String basePath, String applicationName, long reloadIntervalSeconds) {
        this.client = client;
        this.path = basePath + "/" + applicationName;
        reload();
        PathChildrenCache cache = new PathChildrenCache(this.client, path, true);
        cache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorClient, PathChildrenCacheEvent event) {
                try {
                    if (event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED) {
                        ChildData childData = event.getData();
                        String path = childData.getPath();
                        byte[] data = childData.getData();
                        if (data == null) {
                            data = curatorClient.getData().forPath(path);
                        }
                        if (data == null) {
                            logger.warn("child_added, but data is null, path = {}", path);
                        } else {
                            InstanceInfo instanceInfo = InstanceInfoSerializeUtil.deserialize(data);
                            int index = path.lastIndexOf("/") + 1;
                            String id = path.substring(index);
                            map.put(id, instanceInfo);
                            invokeAddProxyCallback(instanceInfo.getProxy());
                            logger.info("instanceInfo add, path = {}, instanceInfo = {}", path, JSONObject.toJSONString(instanceInfo));
                        }
                    } else if (event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
                        ChildData childData = event.getData();
                        String path = childData.getPath();
                        int index = path.lastIndexOf("/") + 1;
                        String id = path.substring(index);
                        InstanceInfo instanceInfo = map.remove(id);
                        if (instanceInfo != null) {
                            invokeRemoveProxyCallback(instanceInfo.getProxy());
                            logger.info("instanceInfo remove, path = {}, instanceInfo = {}", path, JSONObject.toJSONString(instanceInfo));
                        } else {
                            logger.info("instanceInfo try remove, but not found, path = {}", path);
                        }
                    }
                } catch (Exception e) {
                    logger.error("PathChildrenCache listener error", e);
                }
            }
        });
        try {
            cache.start();
        } catch (Exception e) {
            throw new ZkDiscoveryException(e);
        }
        //兜底逻辑，固定间隔
        long initialDelaySeconds = ThreadLocalRandom.current().nextLong(60) + reloadIntervalSeconds;
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(ZkProxyDiscovery.class)).scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    reload();
                } catch (Exception e) {
                    logger.error("reload error", e);
                }
            }
        }, initialDelaySeconds, reloadIntervalSeconds, TimeUnit.SECONDS);
    }

    private void reload() {
        try {
            List<String> strings = client.getChildren().forPath(path);
            if (strings != null) {
                ConcurrentHashMap<String, InstanceInfo> map = new ConcurrentHashMap<>();
                for (String id : strings) {
                    byte[] data = client.getData().forPath(path + "/" + id);
                    InstanceInfo instanceInfo = InstanceInfoSerializeUtil.deserialize(data);
                    if (instanceInfo != null) {
                        map.put(id, instanceInfo);
                    }
                }
                if (!map.isEmpty()) {
                    this.map = map;
                }
            }
        } catch (Exception e) {
            throw new ZkDiscoveryException(e);
        }
    }

    @Override
    public List<Proxy> findAll() {
        Set<Proxy> proxySet = new HashSet<>();
        for (InstanceInfo instanceInfo : map.values()) {
            proxySet.add(instanceInfo.getProxy());
        }
        return new ArrayList<>(proxySet);
    }
}
