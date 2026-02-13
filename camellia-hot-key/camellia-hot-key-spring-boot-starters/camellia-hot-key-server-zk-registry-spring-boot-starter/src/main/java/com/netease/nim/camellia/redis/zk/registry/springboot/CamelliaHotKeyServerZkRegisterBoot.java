package com.netease.nim.camellia.redis.zk.registry.springboot;

import com.netease.nim.camellia.core.discovery.ServerNode;
import com.netease.nim.camellia.hot.key.server.netty.ServerStatus;
import com.netease.nim.camellia.tools.utils.InetUtils;
import com.netease.nim.camellia.zk.ZkRegistry;
import com.netease.nim.camellia.zk.ZkRegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by caojiajun on 2023/5/15
 */
public class CamelliaHotKeyServerZkRegisterBoot {
    private static final Logger logger = LoggerFactory.getLogger(CamelliaHotKeyServerZkRegisterBoot.class);

    private ZkRegistry registry;

    public CamelliaHotKeyServerZkRegisterBoot(CamelliaHotKeyServerZkRegistryProperties properties, String applicationName, int port) {
        if (!properties.isEnable()) {
            logger.info("camellia hot key server zk registry not enable");
            return;
        }
        if (properties.getZkUrl() == null) {
            logger.warn("zkUrl is null, skip camellia hot key server zk registry");
            return;
        }
        ServerNode addr;
        try {
            String host = properties.getHost();
            if (host == null || host.isEmpty()) {
                boolean preferredHostName = properties.isPreferredHostName();
                if (preferredHostName) {
                    host = InetAddress.getLocalHost().getHostName();
                } else {
                    String ignoredInterfaces = properties.getIgnoredInterfaces();
                    String preferredNetworks = properties.getPreferredNetworks();
                    InetAddress inetAddress = InetUtils.findFirstNonLoopbackAddress(ignoredInterfaces, preferredNetworks);
                    if (inetAddress != null) {
                        host = inetAddress.getHostAddress();
                    } else {
                        host = InetAddress.getLocalHost().getHostAddress();
                    }
                }
            }
            addr = new ServerNode(host, port);
        } catch (UnknownHostException e) {
            throw new ZkRegistryException(e);
        }
        registry = new ZkRegistry(properties.getZkUrl(), properties.getSessionTimeoutMs(),
                properties.getConnectionTimeoutMs(), properties.getBaseSleepTimeMs(), properties.getMaxRetries(),
                properties.getBasePath(), applicationName, addr);

        registry.register();

        ServerStatus.registerOnlineCallback(this::register);
        ServerStatus.registerOfflineCallback(this::deregister);
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

    public ZkRegistry getRegistry() {
        return registry;
    }
}
