package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.redis.base.proxy.ProxyDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.auth.ClientAuthByConfigProvider;
import com.netease.nim.camellia.redis.proxy.auth.ClientAuthProvider;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.conf.FileBasedProxyDynamicConfLoader;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConfLoader;
import com.netease.nim.camellia.redis.proxy.monitor.MonitorCallback;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyBeanFactory;
import com.netease.nim.camellia.redis.proxy.tls.frontend.ProxyFrontendTlsProvider;
import com.netease.nim.camellia.redis.proxy.tls.upstream.ProxyUpstreamTlsProvider;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import com.netease.nim.camellia.redis.proxy.upstream.UpstreamRedisClientTemplateFactory;

import java.util.HashSet;
import java.util.Set;


/**
 * Created by caojiajun on 2020/10/22
 */
public class ConfigInitUtil {

    public static IUpstreamClientTemplateFactory initUpstreamClientTemplateFactory(CamelliaServerProperties serverProperties, CamelliaTranspondProperties transpondProperties) {
        String className = serverProperties.getUpstreamClientTemplateFactoryClassName();
        ProxyBeanFactory proxyBeanFactory = serverProperties.getProxyBeanFactory();
        if (className == null || className.equals(UpstreamRedisClientTemplateFactory.class.getName())) {
            return new UpstreamRedisClientTemplateFactory(transpondProperties, proxyBeanFactory);
        }
        return (IUpstreamClientTemplateFactory) proxyBeanFactory.getBean(BeanInitUtils.parseClass(className));
    }

    public static ClientAuthProvider initClientAuthProvider(CamelliaServerProperties serverProperties) {
        String className = serverProperties.getClientAuthProviderClassName();
        if (className == null || className.equals(ClientAuthByConfigProvider.class.getName())) {
            return new ClientAuthByConfigProvider(serverProperties.getPassword());
        }
        ProxyBeanFactory proxyBeanFactory = serverProperties.getProxyBeanFactory();
        return (ClientAuthProvider) proxyBeanFactory.getBean(BeanInitUtils.parseClass(className));
    }

    public static ProxyDynamicConfLoader initProxyDynamicConfLoader(CamelliaServerProperties serverProperties) {
        String className = serverProperties.getProxyDynamicConfLoaderClassName();
        if (className != null) {
            ProxyBeanFactory proxyBeanFactory = serverProperties.getProxyBeanFactory();
            return (ProxyDynamicConfLoader) proxyBeanFactory.getBean(BeanInitUtils.parseClass(className));
        }
        return new FileBasedProxyDynamicConfLoader();
    }

    public static MonitorCallback initMonitorCallback(CamelliaServerProperties serverProperties) {
        String className = serverProperties.getMonitorCallbackClassName();
        if (className != null) {
            ProxyBeanFactory proxyBeanFactory = serverProperties.getProxyBeanFactory();
            return (MonitorCallback) proxyBeanFactory.getBean(BeanInitUtils.parseClass(className));
        }
        return null;
    }

    public static ProxyDiscoveryFactory initProxyDiscoveryFactory(CamelliaTranspondProperties.RedisConfProperties redisConfProperties, ProxyBeanFactory proxyBeanFactory) {
        String className = redisConfProperties.getProxyDiscoveryFactoryClassName();
        if (className != null) {
            return (ProxyDiscoveryFactory) proxyBeanFactory.getBean(BeanInitUtils.parseClass(className));
        }
        return null;
    }

    public static ProxyFrontendTlsProvider initProxyFrontendTlsProvider(CamelliaServerProperties properties) {
        String className = properties.getProxyFrontendTlsProviderClassName();
        if (className != null) {
            ProxyBeanFactory proxyBeanFactory = properties.getProxyBeanFactory();
            return (ProxyFrontendTlsProvider) proxyBeanFactory.getBean(BeanInitUtils.parseClass(className));
        }
        return null;
    }

    public static ProxyUpstreamTlsProvider initProxyUpstreamTlsProvider(CamelliaTranspondProperties properties, ProxyBeanFactory proxyBeanFactory) {
        String className = properties.getRedisConf().getProxyUpstreamTlsProviderClassName();
        if (className != null) {
            return (ProxyUpstreamTlsProvider) proxyBeanFactory.getBean(BeanInitUtils.parseClass(className));
        }
        return null;
    }

    public static Set<Integer> proxyProtocolPorts(CamelliaServerProperties properties, int port, int tlsPort) {
        Set<Integer> set = new HashSet<>();
        String ports = properties.getProxyProtocolPorts();
        if (ports == null || ports.trim().length() == 0) {
            if (port > 0) {
                set.add(port);
            }
            if (tlsPort > 0) {
                set.add(tlsPort);
            }
        } else {
            String[] split = ports.trim().split(",");
            for (String str : split) {
                if (port > 0 && str.trim().equalsIgnoreCase(String.valueOf(port))) {
                    set.add(port);
                }
                if (tlsPort > 0 && str.trim().equalsIgnoreCase(String.valueOf(tlsPort))) {
                    set.add(tlsPort);
                }
            }
        }
        return set;
    }
}
