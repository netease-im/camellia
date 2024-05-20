package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.http.console.CamelliaHttpConsoleConfig;
import com.netease.nim.camellia.http.console.CamelliaHttpConsoleServer;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.console.ConsoleService;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.springboot.conf.CamelliaRedisProxyProperties;
import com.netease.nim.camellia.redis.proxy.util.SocketUtils;
import io.netty.channel.ChannelFuture;
import org.springframework.beans.factory.annotation.Autowired;


/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class CamelliaConsoleServerBoot {

    @Autowired
    private CamelliaRedisProxyProperties properties;

    @Autowired
    private ConsoleService consoleService;

    public void start() throws Exception {
        if (properties.getConsolePort() == 0) {
            return;
        }
        CamelliaHttpConsoleConfig config = new CamelliaHttpConsoleConfig();
        if (properties.getConsolePort() == Constants.Server.consolePortRandSig) {
            config.setPort(SocketUtils.findRandomAvailablePort());
        } else {
            config.setPort(properties.getConsolePort());
        }
        config.setConsoleService(consoleService);
        CamelliaHttpConsoleServer consoleServer = new CamelliaHttpConsoleServer(config);
        ChannelFuture future = consoleServer.start();
        GlobalRedisProxyEnv.setConsolePort(config.getPort());
        GlobalRedisProxyEnv.getProxyShutdown().setConsoleFuture(future);
    }
}
