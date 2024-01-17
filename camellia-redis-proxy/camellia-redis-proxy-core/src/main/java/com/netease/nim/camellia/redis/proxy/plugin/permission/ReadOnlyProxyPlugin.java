package com.netease.nim.camellia.redis.proxy.plugin.permission;

import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.plugin.*;

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
        RedisCommand redisCommand = request.getCommand().getRedisCommand();
        if (redisCommand.getType() != RedisCommand.Type.READ) {
            return FORBIDDEN;
        }
        return ProxyPluginResponse.SUCCESS;
    }
}
