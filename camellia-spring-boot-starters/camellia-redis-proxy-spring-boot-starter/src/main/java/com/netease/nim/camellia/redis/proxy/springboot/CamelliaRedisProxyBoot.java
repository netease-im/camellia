package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.core.api.CamelliaApiEnv;
import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncCommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.springboot.conf.CamelliaRedisProxyProperties;
import com.netease.nim.camellia.redis.proxy.springboot.conf.NettyProperties;
import com.netease.nim.camellia.redis.proxy.netty.CamelliaRedisProxyServer;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public class CamelliaRedisProxyBoot {

    public CamelliaRedisProxyBoot(CamelliaRedisProxyProperties properties, CommandInvoker commandInvoker,
                                  String applicationName, int port) throws Exception {
        CamelliaApiEnv.source = applicationName;

        CamelliaServerProperties serverProperties = new CamelliaServerProperties();
        serverProperties.setPort(port);
        serverProperties.setPassword(properties.getPassword());
        serverProperties.setMonitorEnable(properties.isMonitorEnable());
        serverProperties.setMonitorIntervalSeconds(properties.getMonitorIntervalSeconds());
        NettyProperties netty = properties.getNetty();
        serverProperties.setBossThread(netty.getBossThread());
        if (netty.getWorkThread() > 0) {
            serverProperties.setWorkThread(netty.getWorkThread());
        } else {
            if (commandInvoker instanceof AsyncCommandInvoker) {
                serverProperties.setWorkThread(Constants.Server.asyncWorkThread);
            } else {
                serverProperties.setWorkThread(Constants.Server.syncWorkThread);
            }
        }
        serverProperties.setCommandDecodeMaxBatchSize(netty.getCommandDecodeMaxBatchSize());
        serverProperties.setSoBacklog(netty.getSoBacklog());
        serverProperties.setSoRcvbuf(netty.getSoRcvbuf());
        serverProperties.setSoSndbuf(netty.getSoSndbuf());
        serverProperties.setWriteBufferWaterMarkLow(netty.getWriteBufferWaterMarkLow());
        serverProperties.setWriteBufferWaterMarkHigh(netty.getWriteBufferWaterMarkHigh());

        CamelliaRedisProxyServer server = new CamelliaRedisProxyServer(serverProperties, commandInvoker);
        server.start();
    }
}
