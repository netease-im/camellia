package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.core.client.env.ShardingFunc;
import com.netease.nim.camellia.core.discovery.CamelliaDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.cluster.provider.ConsensusLeaderSelector;
import com.netease.nim.camellia.redis.proxy.cluster.provider.DefaultClusterModeProvider;
import com.netease.nim.camellia.redis.proxy.cluster.provider.ProxyClusterModeProvider;
import com.netease.nim.camellia.redis.proxy.cluster.provider.RedisConsensusLeaderSelector;
import com.netease.nim.camellia.redis.proxy.command.*;
import com.netease.nim.camellia.redis.proxy.conf.*;
import com.netease.nim.camellia.redis.proxy.monitor.LoggingMonitorCallback;
import com.netease.nim.camellia.redis.proxy.monitor.MonitorCallback;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyBeanFactory;
import com.netease.nim.camellia.redis.proxy.route.RouteConfProvider;
import com.netease.nim.camellia.redis.proxy.route.RouteConfProviderEnums;
import com.netease.nim.camellia.redis.proxy.sentinel.DefaultSentinelModeProvider;
import com.netease.nim.camellia.redis.proxy.sentinel.SentinelModeProvider;
import com.netease.nim.camellia.redis.proxy.sentinel.SentinelModeProcessor;
import com.netease.nim.camellia.redis.proxy.tls.frontend.DefaultServerTlsProvider;
import com.netease.nim.camellia.redis.proxy.tls.frontend.ServerTlsProvider;
import com.netease.nim.camellia.redis.proxy.tls.upstream.DefaultUpstreamTlsProvider;
import com.netease.nim.camellia.redis.proxy.tls.upstream.UpstreamTlsProvider;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import com.netease.nim.camellia.redis.proxy.upstream.UpstreamRedisClientTemplateFactory;
import com.netease.nim.camellia.redis.proxy.upstream.connection.DefaultUpstreamAddrConverter;
import com.netease.nim.camellia.redis.proxy.upstream.connection.UpstreamAddrConverter;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;



/**
 * Created by caojiajun on 2020/10/22
 */
public class ConfigInitUtil {

    public static SentinelModeProvider initSentinelModeNodesProvider() {
        String className = BeanInitUtils.getClassName("sentinel.mode.provider", DefaultSentinelModeProvider.class.getName());
        if (className.equalsIgnoreCase(DefaultSentinelModeProvider.class.getName())) {
            return new DefaultSentinelModeProvider();
        } else {
            return (SentinelModeProvider) ServerConf.getProxyBeanFactory().getBean(BeanInitUtils.parseClass(className));
        }
    }

    public static ConsensusLeaderSelector initConsensusLeaderSelector() {
        String className = BeanInitUtils.getClassName("cluster.mode.consensus.leader.selector", RedisConsensusLeaderSelector.class.getName());
        if (className.equals(RedisConsensusLeaderSelector.class.getName())) {
            return new RedisConsensusLeaderSelector();
        } else {
            return (ConsensusLeaderSelector) ServerConf.getProxyBeanFactory().getBean(BeanInitUtils.parseClass(className));
        }
    }

    public static ProxyClusterModeProvider initProxyClusterModeProvider() {
        String className = BeanInitUtils.getClassName("cluster.mode.provider", DefaultClusterModeProvider.class.getName());
        ProxyBeanFactory proxyBeanFactory = ServerConf.getProxyBeanFactory();
        return (ProxyClusterModeProvider) proxyBeanFactory.getBean(BeanInitUtils.parseClass(className));
    }

    public static ProxyNodesDiscovery initProxyNodesDiscovery(ClusterModeProcessor proxyClusterModeProcessor, SentinelModeProcessor proxySentinelModeProcessor) {
        String className = BeanInitUtils.getClassName("proxy.nodes.discovery", DefaultProxyNodesDiscovery.class.getName());
        if (className.equals(DefaultProxyNodesDiscovery.class.getName())) {
            return new DefaultProxyNodesDiscovery(proxyClusterModeProcessor, proxySentinelModeProcessor);
        } else if (className.equals(RedisProxyNodesDiscovery.class.getName())) {
            return new RedisProxyNodesDiscovery(proxyClusterModeProcessor, proxySentinelModeProcessor);
        } else {
            ProxyBeanFactory proxyBeanFactory = ServerConf.getProxyBeanFactory();
            return (ProxyNodesDiscovery) proxyBeanFactory.getBean(BeanInitUtils.parseClass(className));
        }
    }

    public static IUpstreamClientTemplateFactory initUpstreamClientTemplateFactory(RouteConfProvider provider) {
        String className = BeanInitUtils.getClassName("upstream.client.template.factory", UpstreamRedisClientTemplateFactory.class.getName());
        ProxyBeanFactory proxyBeanFactory = ServerConf.getProxyBeanFactory();
        if (className == null || className.equals(UpstreamRedisClientTemplateFactory.class.getName())) {
            return new UpstreamRedisClientTemplateFactory(provider);
        }
        return (IUpstreamClientTemplateFactory) proxyBeanFactory.getBean(BeanInitUtils.parseClass(className));
    }

    public static MonitorCallback initMonitorCallback() {
        String className = BeanInitUtils.getClassName("monitor.callback", LoggingMonitorCallback.class.getName());
        if (className != null) {
            ProxyBeanFactory proxyBeanFactory = ServerConf.getProxyBeanFactory();
            return (MonitorCallback) proxyBeanFactory.getBean(BeanInitUtils.parseClass(className));
        }
        return null;
    }

    public static CamelliaDiscoveryFactory initProxyDiscoveryFactory() {
        String className = BeanInitUtils.getClassName("proxy.discovery.factory", null);
        if (className != null) {
            return (CamelliaDiscoveryFactory) ServerConf.getProxyBeanFactory().getBean(BeanInitUtils.parseClass(className));
        }
        return null;
    }

    public static ServerTlsProvider initProxyFrontendTlsProvider() {
        String className = BeanInitUtils.getClassName("server.tls.provider", DefaultServerTlsProvider.class.getName());
        if (className != null) {
            ProxyBeanFactory proxyBeanFactory = ServerConf.getProxyBeanFactory();
            return (ServerTlsProvider) proxyBeanFactory.getBean(BeanInitUtils.parseClass(className));
        }
        return null;
    }

    public static UpstreamTlsProvider initProxyUpstreamTlsProvider() {
        String className = BeanInitUtils.getClassName("upstream.tls.provider", DefaultUpstreamTlsProvider.class.getName());
        if (className != null) {
            return (UpstreamTlsProvider) ServerConf.getProxyBeanFactory().getBean(BeanInitUtils.parseClass(className));
        }
        return null;
    }

    public static UpstreamAddrConverter initUpstreamAddrConverter() {
        String className = BeanInitUtils.getClassName("upstream.addr.converter", DefaultUpstreamAddrConverter.class.getName());
        if (className != null) {
            return (UpstreamAddrConverter) ServerConf.getProxyBeanFactory().getBean(BeanInitUtils.parseClass(className));
        }
        return null;
    }

    public static QueueFactory initQueueFactory() {
        String className = BeanInitUtils.getClassName("queue.factory", DefaultQueueFactory.class.getName());
        return (QueueFactory) ServerConf.getProxyBeanFactory().getBean(BeanInitUtils.parseClass(className));
    }

    public static ShardingFunc initShardingFunc() {
        String className = BeanInitUtils.getClassName("sharding.func", null);
        if (className == null) {
            return null;
        }
        return (ShardingFunc) ServerConf.getProxyBeanFactory().getBean(BeanInitUtils.parseClass(className));
    }

    public static RouteConfProvider initRouteConfProvider() {
        try {
            String providerName = ProxyDynamicConf.getString("route.conf.provider", RouteConfProviderEnums.simple_tenant.getName());
            if (providerName == null) {
                return null;
            }
            for (RouteConfProviderEnums value : RouteConfProviderEnums.values()) {
                if (value.getName().equalsIgnoreCase(providerName)) {
                    return value.getClazz().getConstructor().newInstance();
                }
            }
            return (RouteConfProvider) ServerConf.getProxyBeanFactory().getBean(BeanInitUtils.parseClass(providerName));
        } catch (Exception e) {
            throw new IllegalArgumentException("init route config provider error", e);
        }
    }

    public static KVClient initKVClient(String namespace) {
        String className = RedisKvConf.getClassName(namespace, "kv.client", null);
        return (KVClient) ServerConf.getProxyBeanFactory().getBean(BeanInitUtils.parseClass(className));
    }
}
