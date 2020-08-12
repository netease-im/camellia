package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.core.api.CamelliaApiEnv;
import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.netty.CamelliaRedisProxyServer;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public class CamelliaRedisProxyBoot {

    private final String applicationName;
    private final int port;

    public CamelliaRedisProxyBoot(CamelliaServerProperties serverProperties,
                                  CommandInvoker commandInvoker, String applicationName) throws Exception {
        CamelliaApiEnv.source = applicationName;
        this.applicationName = applicationName;
        this.port = serverProperties.getPort();
        CamelliaRedisProxyServer server = new CamelliaRedisProxyServer(serverProperties, commandInvoker);
        server.start();
    }

    public String getApplicationName() {
        return applicationName;
    }

    public int getPort() {
        return port;
    }
}
