package com.netease.nim.camellia.redis.proxy.command.async.interceptor;


import com.netease.nim.camellia.redis.proxy.command.Command;

/**
 *
 * Created by caojiajun on 2020/8/10
 */
public class DummyCommandInterceptor implements CommandInterceptor {

    @Override
    public CommandInterceptResponse check(Command command) {
        return CommandInterceptResponse.SUCCESS;
    }
}
