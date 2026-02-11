package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.RemoteMonitor;
import com.netease.nim.camellia.redis.proxy.upstream.UpstreamRedisClientTemplate;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2026/2/11
 */
public class DashboardCommandMonitor implements CommandMonitor {

    private final CamelliaApi camelliaApi;
    private final ConcurrentHashMap<String, RemoteMonitor> monitorMap = new ConcurrentHashMap<>();

    public DashboardCommandMonitor(CamelliaApi camelliaApi) {
        this.camelliaApi = camelliaApi;
    }

    @Override
    public void incrWrite(long bid, String bgroup, String resource, String command) {
        String key = bid + "|" + bgroup;
        RemoteMonitor remoteMonitor = monitorMap.get(key);
        if (remoteMonitor == null) {
            remoteMonitor = monitorMap.computeIfAbsent(key, k -> new RemoteMonitor(bid, bgroup, camelliaApi));
        }
        remoteMonitor.incrWrite(resource, UpstreamRedisClientTemplate.class.getSimpleName(), command);
    }

    @Override
    public void incrRead(long bid, String bgroup, String resource, String command) {
        String key = bid + "|" + bgroup;
        RemoteMonitor remoteMonitor = monitorMap.get(key);
        if (remoteMonitor == null) {
            remoteMonitor = monitorMap.computeIfAbsent(key, k -> new RemoteMonitor(bid, bgroup, camelliaApi));
        }
        remoteMonitor.incrRead(resource, UpstreamRedisClientTemplate.class.getSimpleName(), command);
    }
}
