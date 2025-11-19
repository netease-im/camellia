package com.netease.nim.camellia.redis.base.proxy;

import com.netease.nim.camellia.naming.core.ICamelliaNamingCallback;
import com.netease.nim.camellia.naming.core.ICamelliaNamingService;
import com.netease.nim.camellia.naming.core.InstanceInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2025/11/19
 */
public class CamelliaNamingProxyDiscovery implements IProxyDiscovery {

    private final ICamelliaNamingService namingService;
    private final String serviceName;
    private final ConcurrentHashMap<Callback<Proxy>, String> callbackMap = new ConcurrentHashMap<>();

    public CamelliaNamingProxyDiscovery(String serviceName, ICamelliaNamingService namingService) {
        this.namingService = namingService;
        this.serviceName = serviceName;
    }

    @Override
    public List<Proxy> findAll() {
        List<InstanceInfo> instanceInfoList = namingService.getInstanceInfoList(serviceName);
        List<Proxy> proxyList = new ArrayList<>();
        for (InstanceInfo info : instanceInfoList) {
            proxyList.add(toProxy(info));
        }
        return proxyList;
    }

    private Proxy toProxy(InstanceInfo instanceInfo) {
        Proxy proxy = new Proxy();
        proxy.setHost(instanceInfo.getHost());
        proxy.setPort(instanceInfo.getPort());
        return proxy;
    }

    @Override
    public void setCallback(Callback<Proxy> callback) {
        String id = namingService.subscribe(serviceName, new ICamelliaNamingCallback() {
            @Override
            public void add(List<InstanceInfo> list) {
                for (InstanceInfo info : list) {
                    callback.add(toProxy(info));
                }
            }

            @Override
            public void remove(List<InstanceInfo> list) {
                for (InstanceInfo info : list) {
                    callback.remove(toProxy(info));
                }
            }
        });
        callbackMap.put(callback, id);
    }

    @Override
    public void clearCallback(Callback<Proxy> callback) {
        String id = callbackMap.remove(callback);
        if (id != null) {
            namingService.unsubscribe(serviceName, id);
        }
    }
}
