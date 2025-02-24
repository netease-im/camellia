package com.netease.nim.camellia.redis.proxy.plugin.permission;

import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

/**
 * Created by caojiajun on 2024/1/17
 */
public class ReadOnlyProxyPlugin implements ProxyPlugin {

    private static final ProxyPluginResponse FORBIDDEN = new ProxyPluginResponse(false, "ERR write command forbidden");

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return BuildInProxyPluginEnum.READ_ONLY_PLUGIN.getRequestOrder();
            }

            @Override
            public int reply() {
                return BuildInProxyPluginEnum.READ_ONLY_PLUGIN.getReplyOrder();
            }
        };
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest request) {
        try {
            CommandContext commandContext = request.getCommand().getCommandContext();
            Long bid = commandContext.getBid();
            String bgroup = commandContext.getBgroup();
            boolean enable = ProxyDynamicConf.getBoolean("read.only.plugin.enable", bid, bgroup, true);
            if (!enable) {
                return ProxyPluginResponse.SUCCESS;
            }
            RedisCommand redisCommand = request.getCommand().getRedisCommand();
            if (redisCommand.getType() != RedisCommand.Type.READ) {
                return FORBIDDEN;
            }
            return ProxyPluginResponse.SUCCESS;
        } catch (Exception e) {
            ErrorLogCollector.collect(ReadOnlyProxyPlugin.class, "read only plugin execute error", e);
            return ProxyPluginResponse.SUCCESS;
        }
    }
}
