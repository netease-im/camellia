package com.netease.nim.camellia.redis.base.proxy;

import com.netease.nim.camellia.naming.core.ICamelliaNamingCallback;
import com.netease.nim.camellia.naming.core.ICamelliaNamingService;
import com.netease.nim.camellia.naming.core.InstanceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2025/11/19
 */
public class CamelliaNamingProxyDiscovery implements IProxyDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaNamingProxyDiscovery.class);

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
                    try {
                        callback.add(toProxy(info));
                    } catch (Exception e) {
                        logger.error("add error, proxy = {}", toProxy(info), e);
                    }
                }
            }

            @Override
            public void remove(List<InstanceInfo> list) {
                for (InstanceInfo info : list) {
                    try {
                        callback.remove(toProxy(info));
                    } catch (Exception e) {
                        logger.error("remove error, proxy = {}", toProxy(info), e);
                    }
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
