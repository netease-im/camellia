package com.netease.nim.camellia.zk;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.discovery.AbstractCamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.ServerNode;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2020/8/10
 */
public class ZkDiscovery extends AbstractCamelliaDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(ZkDiscovery.class);

    private final String applicationName;
    private final CuratorFramework client;
    private final String path;
    private ConcurrentHashMap<String, ServerNode> map = new ConcurrentHashMap<>();

    public ZkDiscovery(String zkUrl, String basePath, String applicationName) {
        this(ZkClientFactory.DEFAULT.getClient(zkUrl), basePath, applicationName, ZkConstants.reloadIntervalSeconds);
    }

    public ZkDiscovery(CuratorFramework client, String basePath, String applicationName, long reloadIntervalSeconds) {
        this.applicationName = applicationName;
        this.client = client;
        this.path = basePath + "/" + applicationName;
        reload();
        PathChildrenCache cache = new PathChildrenCache(this.client, path, true);
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
                        logger.warn("child_added, but data is null, path = {}", path);
                    } else {
                        ServerNode serverNode = InstanceInfoSerializeUtil.deserialize(data);
                        int index = path.lastIndexOf("/") + 1;
                        String id = path.substring(index);
                        map.put(id, serverNode);
                        invokeAddCallback(serverNode);
                        logger.info("instanceInfo add, path = {}, instanceInfo = {}", path, JSONObject.toJSONString(serverNode));
                    }
                } else if (event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
                    ChildData childData = event.getData();
                    String path = childData.getPath();
                    int index = path.lastIndexOf("/") + 1;
                    String id = path.substring(index);
                    ServerNode serverNode = map.remove(id);
                    if (serverNode != null) {
                        invokeRemoveCallback(serverNode);
                        logger.info("instanceInfo remove, path = {}, instanceInfo = {}", path, JSONObject.toJSONString(serverNode));
                    } else {
                        logger.info("instanceInfo try remove, but not found, path = {}", path);
                    }
                }
            } catch (Exception e) {
                logger.error("PathChildrenCache listener error", e);
            }
        });
        try {
            cache.start();
        } catch (Exception e) {
            throw new ZkDiscoveryException(e);
        }
        //兜底逻辑，固定间隔
        long initialDelaySeconds = ThreadLocalRandom.current().nextLong(60) + reloadIntervalSeconds;
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(ZkDiscovery.class)).scheduleAtFixedRate(() -> {
            try {
                reload();
            } catch (Exception e) {
                logger.error("reload error", e);
            }
        }, initialDelaySeconds, reloadIntervalSeconds, TimeUnit.SECONDS);
    }

    private void reload() {
        try {
            List<String> strings = client.getChildren().forPath(path);
            if (strings != null) {
                ConcurrentHashMap<String, ServerNode> map = new ConcurrentHashMap<>();
                for (String id : strings) {
                    byte[] data = client.getData().forPath(path + "/" + id);
                    ServerNode serverNode = InstanceInfoSerializeUtil.deserialize(data);
                    if (serverNode != null) {
                        map.put(id, serverNode);
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
    public List<ServerNode> findAll() {
        return new ArrayList<>(map.values());
    }

    public String getApplicationName() {
        return applicationName;
    }
}
