package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.http.console.CamelliaHttpConsoleConfig;
import com.netease.nim.camellia.http.console.CamelliaHttpConsoleServer;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.conf.ServerConf;
import com.netease.nim.camellia.redis.proxy.console.ConsoleService;
import com.netease.nim.camellia.redis.proxy.conf.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.util.SocketUtils;
import io.netty.channel.ChannelFuture;


/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class CamelliaConsoleServerBoot {

    private final ConsoleService consoleService;

    public CamelliaConsoleServerBoot(ConsoleService consoleService) {
        this.consoleService = consoleService;
    }

    public void start() {
        int port = ServerConf.consolePort();
        CamelliaHttpConsoleConfig config = new CamelliaHttpConsoleConfig();
        if (port == Constants.Server.consolePortRandSig) {
            config.setPort(SocketUtils.findRandomAvailablePort());
        } else {
            config.setPort(port);
        }
        config.setConsoleService(consoleService);
        CamelliaHttpConsoleServer consoleServer = new CamelliaHttpConsoleServer(config);
        ChannelFuture future = consoleServer.start();
        GlobalRedisProxyEnv.setConsolePort(config.getPort());
        GlobalRedisProxyEnv.getProxyShutdown().setConsoleFuture(future);
    }
}
