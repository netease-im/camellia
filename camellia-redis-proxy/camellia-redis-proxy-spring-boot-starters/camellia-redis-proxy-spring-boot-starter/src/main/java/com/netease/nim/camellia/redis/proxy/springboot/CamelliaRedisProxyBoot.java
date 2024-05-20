package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.core.api.CamelliaApiEnv;
import com.netease.nim.camellia.redis.proxy.command.ICommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.netty.CamelliaRedisProxyServer;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public class CamelliaRedisProxyBoot {

    private final String applicationName;
    private final int port;
    private final int tlsPort;

    public CamelliaRedisProxyBoot(CamelliaServerProperties serverProperties,
                                  ICommandInvoker commandInvoker, CamelliaConsoleServerBoot consoleServerBoot) throws Exception {
        CamelliaApiEnv.source = serverProperties.getApplicationName();
        this.applicationName = serverProperties.getApplicationName();
        CamelliaRedisProxyServer server = new CamelliaRedisProxyServer(serverProperties, commandInvoker);
        server.start();
        consoleServerBoot.start();
        this.port = server.getPort();
        this.tlsPort = server.getTlsPort();
    }

    public String getApplicationName() {
        return applicationName;
    }

    public int getPort() {
        return port;
    }

    public int getTlsPort() {
        return tlsPort;
    }
}
