package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.core.api.CamelliaApiEnv;
import com.netease.nim.camellia.redis.proxy.conf.ServerConf;
import com.netease.nim.camellia.redis.proxy.netty.CamelliaRedisProxyServer;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public class CamelliaRedisProxyBoot {

    private final String applicationName;
    private final int port;
    private final int tlsPort;

    public CamelliaRedisProxyBoot(CamelliaConsoleServerBoot consoleServerBoot) throws Exception {
        CamelliaApiEnv.source = ServerConf.getApplicationName();
        this.applicationName = ServerConf.getApplicationName();
        CamelliaRedisProxyServer server = new CamelliaRedisProxyServer();
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
