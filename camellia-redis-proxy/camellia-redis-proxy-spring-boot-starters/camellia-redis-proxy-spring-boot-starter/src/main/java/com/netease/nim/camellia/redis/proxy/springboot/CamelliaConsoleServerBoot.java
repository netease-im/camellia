package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.http.console.CamelliaHttpConsoleConfig;
import com.netease.nim.camellia.http.console.CamelliaHttpConsoleServer;
import com.netease.nim.camellia.redis.proxy.console.ConsoleService;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.springboot.conf.CamelliaRedisProxyProperties;
import io.netty.channel.ChannelFuture;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class CamelliaConsoleServerBoot {

    @Autowired
    private CamelliaRedisProxyProperties properties;

    @Autowired
    private ConsoleService consoleService;

    @PostConstruct
    public void init() throws Exception {
        CamelliaHttpConsoleConfig config = new CamelliaHttpConsoleConfig();
        config.setPort(properties.getConsolePort());
        config.setConsoleService(consoleService);
        CamelliaHttpConsoleServer consoleServer = new CamelliaHttpConsoleServer(config);
        ChannelFuture future = consoleServer.start();
        GlobalRedisProxyEnv.setConsolePort(config.getPort());
        GlobalRedisProxyEnv.getProxyShutdown().setConsoleChannelFuture(future);
    }
}
