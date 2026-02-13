package com.netease.nim.camellia.naming.core;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.ServerNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2026/2/13
 */
public class CamelliaNamingDiscovery implements CamelliaDiscovery {

    private final String serviceName;
    private final ICamelliaNamingService namingService;

    private final ConcurrentHashMap<Callback, String> callbackMap = new ConcurrentHashMap<>();

    public CamelliaNamingDiscovery(ICamelliaNamingService namingService, String serviceName) {
        this.serviceName = serviceName;
        this.namingService = namingService;
    }

    @Override
    public List<ServerNode> findAll() {
        return toServerNodeList(namingService.getInstanceInfoList(serviceName));
    }

    private List<ServerNode> toServerNodeList(List<InstanceInfo> list) {
        if (list == null) {
            return null;
        }
        List<ServerNode> result = new ArrayList<>(list.size());
        for (InstanceInfo instanceInfo : list) {
            result.add(new ServerNode(instanceInfo.getHost(), instanceInfo.getPort()));
        }
        return result;
    }

    private ServerNode toServerNode(InstanceInfo instanceInfo) {
        return new ServerNode(instanceInfo.getHost(), instanceInfo.getPort());
    }

    @Override
    public void setCallback(Callback callback) {
        String id = namingService.subscribe(serviceName, new ICamelliaNamingCallback() {
            @Override
            public void add(List<InstanceInfo> list) {
                for (InstanceInfo instanceInfo : list) {
                    callback.add(toServerNode(instanceInfo));
                }
            }

            @Override
            public void remove(List<InstanceInfo> list) {
                for (InstanceInfo instanceInfo : list) {
                    callback.remove(toServerNode(instanceInfo));
                }
            }
        });
        callbackMap.put(callback, id);
    }

    @Override
    public void clearCallback(Callback callback) {
        String id = callbackMap.remove(callback);
        namingService.unsubscribe(serviceName, id);
    }
}
