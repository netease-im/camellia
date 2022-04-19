package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.ProxyDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.discovery.common.IProxyDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2022/4/19
 */
public class CamelliaProxyDiscoveryFactory {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaProxyDiscoveryFactory.class);

    private static ProxyDiscoveryFactory factory;

    public static synchronized void init(ProxyDiscoveryFactory factory) {
        if (factory == null) {
            return;
        }
        if (CamelliaProxyDiscoveryFactory.factory != null) {
            logger.warn("init CamelliaProxyDiscoveryFactory fail, factory has init");
            return;
        }
        CamelliaProxyDiscoveryFactory.factory = factory;
        logger.info("init CamelliaProxyDiscoveryFactory success, factory = {}", factory.getClass().getName());
    }

    public static IProxyDiscovery getProxyDiscovery(String proxyName) {
        if (factory == null) return null;
        return factory.getProxyDiscovery(proxyName);
    }
}
