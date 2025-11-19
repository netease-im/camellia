package com.netease.nim.camellia.hot.key.sdk.discovery;

import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerAddr;
import com.netease.nim.camellia.naming.core.ICamelliaNamingCallback;
import com.netease.nim.camellia.naming.core.ICamelliaNamingService;
import com.netease.nim.camellia.naming.core.InstanceInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2025/11/19
 */
public class CamelliaNamingHotKeyServerDiscovery implements HotKeyServerDiscovery {

    private final ICamelliaNamingService namingService;
    private final String serviceName;
    private final ConcurrentHashMap<Callback<HotKeyServerAddr>, String> callbackMap = new ConcurrentHashMap<>();

    public CamelliaNamingHotKeyServerDiscovery(ICamelliaNamingService namingService, String serviceName) {
        this.namingService = namingService;
        this.serviceName = serviceName;
    }

    @Override
    public String getName() {
        return serviceName;
    }

    @Override
    public List<HotKeyServerAddr> findAll() {
        List<InstanceInfo> instanceInfoList = namingService.getInstanceInfoList(serviceName);
        List<HotKeyServerAddr> list = new ArrayList<>();
        for (InstanceInfo info : instanceInfoList) {
            list.add(toAddr(info));
        }
        return list;
    }

    private HotKeyServerAddr toAddr(InstanceInfo instanceInfo) {
        return new HotKeyServerAddr(instanceInfo.getHost(), instanceInfo.getPort());
    }

    @Override
    public void setCallback(Callback<HotKeyServerAddr> callback) {
        String id = namingService.subscribe(serviceName, new ICamelliaNamingCallback() {
            @Override
            public void add(List<InstanceInfo> list) {
                for (InstanceInfo info : list) {
                    callback.add(toAddr(info));
                }
            }

            @Override
            public void remove(List<InstanceInfo> list) {
                for (InstanceInfo info : list) {
                    callback.remove(toAddr(info));
                }
            }
        });
        callbackMap.put(callback, id);
    }

    @Override
    public void clearCallback(Callback<HotKeyServerAddr> callback) {
        String id = callbackMap.remove(callback);
        if (id != null) {
            namingService.unsubscribe(serviceName, id);
        }
    }
}
