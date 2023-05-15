package com.netease.nim.camellia.redis.zk.registry.springboot;

import com.netease.nim.camellia.redis.base.proxy.Proxy;
import com.netease.nim.camellia.redis.proxy.discovery.zk.ZkProxyRegistry;
import com.netease.nim.camellia.tools.utils.InetUtils;
import com.netease.nim.camellia.zk.ZkRegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;

/**
 *
 * Created by caojiajun on 2020/8/12
 */
public class CamelliaRedisProxyZkRegisterBoot {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisProxyZkRegisterBoot.class);

    private ZkProxyRegistry registry;

    public CamelliaRedisProxyZkRegisterBoot(CamelliaRedisProxyZkRegistryProperties properties, String applicationName, int port) {
        if (!properties.isEnable()) {
            logger.info("camellia redis proxy zk registry not enable");
            return;
        }
        if (properties.getZkUrl() == null) {
            logger.warn("zkUrl is null, skip camellia redis proxy zk registry");
            return;
        }
        Proxy proxy;
        try {
            proxy = new Proxy();
            proxy.setPort(port);
            String host = properties.getHost();
            if (host != null && host.length() > 0) {
                proxy.setHost(host);
            } else {
                boolean preferredHostName = properties.isPreferredHostName();
                if (preferredHostName) {
                    proxy.setHost(InetAddress.getLocalHost().getHostName());
                } else {
                    String ignoredInterfaces = properties.getIgnoredInterfaces();
                    if (ignoredInterfaces != null) {
                        String[] interfaces = ignoredInterfaces.split(",");
                        Collections.addAll(InetUtils.ignoredInterfaces, interfaces);
                    }
                    String preferredNetworks = properties.getPreferredNetworks();
                    if (preferredNetworks != null) {
                        String[] networks = preferredNetworks.split(",");
                        Collections.addAll(InetUtils.preferredNetworks, networks);
                    }
                    InetAddress inetAddress = InetUtils.findFirstNonLoopbackAddress();
                    if (inetAddress != null) {
                        proxy.setHost(inetAddress.getHostAddress());
                    } else {
                        proxy.setHost(InetAddress.getLocalHost().getHostAddress());
                    }
                }
            }
        } catch (UnknownHostException e) {
            throw new ZkRegistryException(e);
        }
        registry = new ZkProxyRegistry(properties.getZkUrl(), properties.getSessionTimeoutMs(),
                properties.getConnectionTimeoutMs(), properties.getBaseSleepTimeMs(), properties.getMaxRetries(),
                properties.getBasePath(), applicationName, proxy);

        registry.register();
    }

    public void register() {
        if (registry != null) {
            registry.register();
        }
    }

    public void deregister() {
        if (registry != null) {
            registry.deregister();
        }
    }

    public ZkProxyRegistry getRegistry() {
        return registry;
    }
}
