package com.netease.nim.camellia.redis.zk.discovery;

import com.netease.nim.camellia.redis.zk.common.ZkConstants;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2020/8/11
 */
public class ZkClientFactory {

    public static ZkClientFactory DEFAULT = new ZkClientFactory();

    private final Map<String, CuratorFramework> map = new HashMap<>();

    private int sessionTimeoutMs = ZkConstants.sessionTimeoutMs;
    private int connectionTimeoutMs = ZkConstants.connectionTimeoutMs;
    private int baseSleepTimeMs = ZkConstants.baseSleepTimeMs;
    private int maxRetries = ZkConstants.maxRetries;

    public ZkClientFactory() {
    }

    public ZkClientFactory(int sessionTimeoutMs, int connectionTimeoutMs, int baseSleepTimeMs, int maxRetries) {
        this.sessionTimeoutMs = sessionTimeoutMs;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.baseSleepTimeMs = baseSleepTimeMs;
        this.maxRetries = maxRetries;
    }

    public CuratorFramework getClient(String zkUrl) {
        CuratorFramework client = map.get(zkUrl);
        if (client == null) {
            synchronized (map) {
                client = map.get(zkUrl);
                if (client == null) {
                    client = CuratorFrameworkFactory.builder()
                            .connectString(zkUrl)
                            .sessionTimeoutMs(sessionTimeoutMs)
                            .connectionTimeoutMs(connectionTimeoutMs)
                            .retryPolicy(new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries))
                            .build();
                    client.start();
                    map.put(zkUrl, client);
                }
            }
        }
        return client;
    }
}
