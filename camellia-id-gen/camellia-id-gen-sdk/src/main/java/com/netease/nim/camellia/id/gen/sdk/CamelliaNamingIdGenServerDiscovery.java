package com.netease.nim.camellia.id.gen.sdk;

import com.netease.nim.camellia.naming.core.ICamelliaNamingCallback;
import com.netease.nim.camellia.naming.core.ICamelliaNamingService;
import com.netease.nim.camellia.naming.core.InstanceInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2025/11/19
 */
public class CamelliaNamingIdGenServerDiscovery implements IdGenServerDiscovery {

    private final ICamelliaNamingService namingService;
    private final String serviceName;
    private final ConcurrentHashMap<Callback<IdGenServer>, String> callbackMap = new ConcurrentHashMap<>();

    public CamelliaNamingIdGenServerDiscovery(String serviceName, ICamelliaNamingService namingService) {
        this.namingService = namingService;
        this.serviceName = serviceName;
    }

    @Override
    public List<IdGenServer> findAll() {
        List<InstanceInfo> instanceInfoList = namingService.getInstanceInfoList(serviceName);
        List<IdGenServer> proxyList = new ArrayList<>();
        for (InstanceInfo info : instanceInfoList) {
            proxyList.add(toServer(info));
        }
        return proxyList;
    }

    private IdGenServer toServer(InstanceInfo instanceInfo) {
        return new IdGenServer("http://" + instanceInfo.getHost() + ":" + instanceInfo.getPort());
    }

    @Override
    public void setCallback(Callback<IdGenServer> callback) {
        String id = namingService.subscribe(serviceName, new ICamelliaNamingCallback() {
            @Override
            public void add(List<InstanceInfo> list) {
                for (InstanceInfo info : list) {
                    callback.add(toServer(info));
                }
            }

            @Override
            public void remove(List<InstanceInfo> list) {
                for (InstanceInfo info : list) {
                    callback.remove(toServer(info));
                }
            }
        });
        callbackMap.put(callback, id);
    }

    @Override
    public void clearCallback(Callback<IdGenServer> callback) {
        String id = callbackMap.remove(callback);
        if (id != null) {
            namingService.unsubscribe(serviceName, id);
        }
    }
}
