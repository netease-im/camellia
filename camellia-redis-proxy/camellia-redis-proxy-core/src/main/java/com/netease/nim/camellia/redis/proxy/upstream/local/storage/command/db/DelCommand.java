package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.db;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.CommandConfig;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.ICommand;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;

/**
 * DEL key
 * <p>
 * Created by caojiajun on 2025/1/10
 */
public class DelCommand extends ICommand {

    public DelCommand(CommandConfig commandConfig) {
        super(commandConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.DEL;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 2;
    }

    @Override
    protected Reply execute(short slot, Command command) throws Exception {
        byte[][] objects = command.getObjects();
        Key key = new Key(objects[1]);
        KeyInfo keyInfo = keyReadWrite.get(slot, key);
        int ret = 0;
        if (keyInfo != null) {
            keyReadWrite.delete(slot, key);
            ret = 1;
        }
        return IntegerReply.parse(ret);
    }
}
