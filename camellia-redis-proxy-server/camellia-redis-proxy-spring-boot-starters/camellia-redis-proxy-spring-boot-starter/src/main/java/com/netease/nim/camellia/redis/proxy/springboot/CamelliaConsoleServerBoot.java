package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.redis.proxy.console.ConsoleServer;
import com.netease.nim.camellia.redis.proxy.console.ConsoleService;
import com.netease.nim.camellia.redis.proxy.springboot.conf.CamelliaRedisProxyProperties;
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
        ConsoleServer consoleServer = new ConsoleServer(properties.getConsolePort(), consoleService);
        consoleServer.start();
    }
}
