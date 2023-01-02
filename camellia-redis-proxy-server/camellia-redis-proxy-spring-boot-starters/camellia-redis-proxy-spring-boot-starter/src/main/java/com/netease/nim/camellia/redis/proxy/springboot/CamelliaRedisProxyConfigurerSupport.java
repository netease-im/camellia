package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.core.client.env.ShardingFunc;
import com.netease.nim.camellia.redis.proxy.ProxyDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyBeanFactory;
import com.netease.nim.camellia.redis.proxy.route.ProxyRouteConfUpdater;
import com.netease.nim.camellia.redis.proxy.auth.ClientAuthProvider;
import com.netease.nim.camellia.redis.proxy.monitor.MonitorCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by caojiajun on 2021/9/29
 */
@Component
public class CamelliaRedisProxyConfigurerSupport {

    @Autowired(required = false)
    public ProxyBeanFactory proxyBeanFactory;

    /**
     * 监控数据回调
     */
    @Autowired(required = false)
    private MonitorCallback monitorCallback;

    /**
     * 自定义auth策略
     */
    @Autowired(required = false)
    private ClientAuthProvider clientAuthProvider;

    /**
     * 自定义分片函数
     */
    @Autowired(required = false)
    private ShardingFunc shardingFunc;

    /**
     * 自定义动态路由
     */
    @Autowired(required = false)
    private ProxyRouteConfUpdater proxyRouteConfUpdater;


    /**
     * 动态配置回调
     */
    @Autowired(required = false)
    private ProxyDynamicConfSupport proxyDynamicConfSupport;

    /**
     * 如果转发的后端也是无状态proxy，如何去发现后端proxy的一个工厂类
     */
    @Autowired(required = false)
    private ProxyDiscoveryFactory proxyDiscoveryFactory;

    public ProxyBeanFactory getProxyBeanFactory() {
        return proxyBeanFactory;
    }

    public MonitorCallback getMonitorCallback() {
        return monitorCallback;
    }

    public ClientAuthProvider getClientAuthProvider() {
        return clientAuthProvider;
    }

    public ShardingFunc getShardingFunc() {
        return shardingFunc;
    }

    public ProxyRouteConfUpdater getProxyRouteConfUpdater() {
        return proxyRouteConfUpdater;
    }

    public ProxyDynamicConfSupport getProxyDynamicConfSupport() {
        return proxyDynamicConfSupport;
    }

    public ProxyDiscoveryFactory getProxyDiscoveryFactory() {
        return proxyDiscoveryFactory;
    }
}
