package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.CommandInterceptResponse;
import com.netease.nim.camellia.redis.proxy.command.async.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;

public class CustomCommandInterceptor implements CommandInterceptor {

    private static final CommandInterceptResponse KEY_TOO_LONG = new CommandInterceptResponse(false, "key too long");
    private static final CommandInterceptResponse VALUE_TOO_LONG = new CommandInterceptResponse(false, "value too long");

    @Override
    public CommandInterceptResponse check(Command command) {
        if (command.getRedisCommand() == RedisCommand.SET) {
            byte[] key = command.getObjects()[1];
            if (key.length > 256) {
                return KEY_TOO_LONG;
            }
            byte[] value = command.getObjects()[2];
            if (value.length > 1024 * 1024 * 5) {
                return VALUE_TOO_LONG;
            }
        }
        return CommandInterceptResponse.SUCCESS;
    }
}
