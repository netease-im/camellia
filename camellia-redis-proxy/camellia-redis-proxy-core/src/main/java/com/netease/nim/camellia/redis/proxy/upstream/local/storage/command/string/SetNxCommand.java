package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.string;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.CommandConfig;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.ICommand;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums.DataType;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;

/**
 * SETNX key value
 * <p>
 * Created by caojiajun on 2025/1/10
 */
public class SetNxCommand extends ICommand {

    public SetNxCommand(CommandConfig commandConfig) {
        super(commandConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SETNX;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 3;
    }

    @Override
    protected Reply execute(short slot, Command command) throws Exception {
        byte[][] objects = command.getObjects();
        Key key = new Key(objects[1]);
        byte[] value = objects[2];

        KeyInfo keyInfo = keyReadWrite.get(slot, key);
        if (keyInfo == null) {
            keyInfo = new KeyInfo(DataType.string, key.key());
            if (key.key().length + value.length <= 128) {
                keyInfo.setExtra(value);
            } else {
                keyInfo.setExtra(null);
                stringReadWrite.put(slot, keyInfo, value);
            }
            return IntegerReply.REPLY_1;
        }
        return IntegerReply.REPLY_0;
    }
}
