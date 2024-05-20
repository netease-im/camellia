package com.netease.nim.camellia.hot.key.server.springboot;

import com.netease.nim.camellia.hot.key.server.console.ConsoleService;
import com.netease.nim.camellia.hot.key.server.springboot.conf.CamelliaHotKeyServerProperties;
import com.netease.nim.camellia.http.console.CamelliaHttpConsoleConfig;
import com.netease.nim.camellia.http.console.CamelliaHttpConsoleServer;
import org.springframework.beans.factory.annotation.Autowired;


/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class CamelliaConsoleServerBoot {

    @Autowired
    private CamelliaHotKeyServerProperties properties;

    @Autowired
    private ConsoleService consoleService;

    public void start() {
        CamelliaHttpConsoleConfig config = new CamelliaHttpConsoleConfig();
        config.setPort(properties.getConsolePort());
        config.setConsoleService(consoleService);
        CamelliaHttpConsoleServer consoleServer = new CamelliaHttpConsoleServer(config);
        consoleServer.start();
    }
}
