package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.core.api.CamelliaApiEnv;
import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.netty.CamelliaRedisProxyServer;
import io.netty.channel.EventLoopGroup;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public class CamelliaRedisProxyBoot {

    private final String applicationName;
    private final int port;

    public CamelliaRedisProxyBoot(CamelliaServerProperties serverProperties, EventLoopGroup bossGroup, EventLoopGroup workGroup,
                                  CommandInvoker commandInvoker) throws Exception {
        CamelliaApiEnv.source = serverProperties.getApplicationName();
        this.applicationName = serverProperties.getApplicationName();
        this.port = serverProperties.getPort();
        CamelliaRedisProxyServer server = new CamelliaRedisProxyServer(serverProperties, bossGroup, workGroup, commandInvoker);
        server.start();
    }

    public String getApplicationName() {
        return applicationName;
    }

    public int getPort() {
        return port;
    }
}
