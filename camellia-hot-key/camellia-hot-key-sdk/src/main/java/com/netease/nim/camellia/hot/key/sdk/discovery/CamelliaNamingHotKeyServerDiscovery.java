package com.netease.nim.camellia.hot.key.sdk.discovery;

import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerAddr;
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
public class CamelliaNamingHotKeyServerDiscovery implements HotKeyServerDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaNamingHotKeyServerDiscovery.class);

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
                    try {
                        callback.add(toAddr(info));
                    } catch (Exception e) {
                        logger.error("add error, addr = {}", toAddr(info), e);
                    }
                }
            }

            @Override
            public void remove(List<InstanceInfo> list) {
                for (InstanceInfo info : list) {
                    try {
                        callback.remove(toAddr(info));
                    } catch (Exception e) {
                        logger.error("remove error, addr = {}", toAddr(info), e);
                    }
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
