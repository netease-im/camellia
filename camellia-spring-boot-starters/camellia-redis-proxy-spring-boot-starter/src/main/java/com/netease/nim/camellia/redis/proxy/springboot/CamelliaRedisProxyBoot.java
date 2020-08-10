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

    public CamelliaRedisProxyBoot(CamelliaServerProperties serverProperties,
                                  CommandInvoker commandInvoker, String applicationName) throws Exception {
        CamelliaApiEnv.source = applicationName;
        CamelliaRedisProxyServer server = new CamelliaRedisProxyServer(serverProperties, commandInvoker);
        server.start();
    }
}
