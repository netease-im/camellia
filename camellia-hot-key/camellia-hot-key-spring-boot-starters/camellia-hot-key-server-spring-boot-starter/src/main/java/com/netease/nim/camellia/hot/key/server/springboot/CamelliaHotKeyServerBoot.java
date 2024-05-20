package com.netease.nim.camellia.hot.key.server.springboot;

import com.netease.nim.camellia.hot.key.server.CamelliaHotKeyServer;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;

/**
 * Created by caojiajun on 2023/5/10
 */
public class CamelliaHotKeyServerBoot {

    private final String applicationName;
    private final int port;

    public CamelliaHotKeyServerBoot(HotKeyServerProperties properties, CamelliaConsoleServerBoot consoleServerBoot) {
        this.applicationName = properties.getApplicationName();
        CamelliaHotKeyServer server = new CamelliaHotKeyServer(properties);
        server.start();
        consoleServerBoot.start();
        this.port = server.getPort();
    }

    public String getApplicationName() {
        return applicationName;
    }

    public int getPort() {
        return port;
    }
}
