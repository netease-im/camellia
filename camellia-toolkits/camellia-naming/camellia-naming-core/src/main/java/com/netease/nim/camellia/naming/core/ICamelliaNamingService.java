package com.netease.nim.camellia.naming.core;

import java.util.List;

/**
 * Created by caojiajun on 2025/11/18
 */
public interface ICamelliaNamingService {

    /**
     * get current instance info
     * @return instance
     */
    InstanceInfo getInstanceInfo();

    /**
     * get an instance info list
     * @param serviceName service name
     * @return list
     */
    List<InstanceInfo> getInstanceInfoList(String serviceName);

    /**
     * subscribe
     * @param serviceName service name
     * @param callback callback
     * @return id
     */
    String subscribe(String serviceName, ICamelliaNamingCallback callback);

    /**
     * unsubscribe
     * @param serviceName service name
     * @param id id
     */
    void unsubscribe(String serviceName, String id);

    /**
     * register
     */
    void register();

    /**
     * deregister
     */
    void deregister();

}
