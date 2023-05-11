package com.netease.nim.camellia.hot.key.server.springboot;

import com.netease.nim.camellia.hot.key.server.console.ConsoleServer;
import com.netease.nim.camellia.hot.key.server.console.ConsoleService;
import com.netease.nim.camellia.hot.key.server.springboot.conf.CamelliaHotKeyServerProperties;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class CamelliaConsoleServerBoot {

    @Autowired
    private CamelliaHotKeyServerProperties properties;

    @Autowired
    private ConsoleService consoleService;

    @PostConstruct
    public void init() throws Exception {
        ConsoleServer consoleServer = new ConsoleServer(properties.getConsolePort(), consoleService);
        consoleServer.start();
    }
}
