package com.netease.nim.camellia.redis.proxy.command.async;


import com.netease.nim.camellia.redis.proxy.command.Command;

/**
 *
 * Created by caojiajun on 2020/8/10
 */
public class DummyCommandFilter implements CommandFilter {

    @Override
    public CommandFilterResponse check(Command command) {
        return CommandFilterResponse.SUCCESS;
    }
}
