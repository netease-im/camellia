package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.db;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.CommandConfig;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.ICommand;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;

/**
 * EXPIRETIME key
 * <p>
 * Created by caojiajun on 2025/1/10
 */
public class ExpireTimeCommand extends ICommand {

    public ExpireTimeCommand(CommandConfig commandConfig) {
        super(commandConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.EXPIRETIME;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 2;
    }

    @Override
    public Reply runToCompletion(short slot, Command command) {
        byte[][] objects = command.getObjects();
        Key key = new Key(objects[1]);
        ValueWrapper<KeyInfo> keyWrapper = keyReadWrite.getForRunToCompletion(slot, key);
        if (keyWrapper == null) {
            return null;
        }
        KeyInfo keyInfo = keyWrapper.get();
        return execute0(keyInfo);
    }

    @Override
    protected Reply execute(short slot, Command command) throws Exception {
        byte[][] objects = command.getObjects();
        Key key = new Key(objects[1]);
        KeyInfo keyInfo = keyReadWrite.get(slot, key);
        return execute0(keyInfo);
    }

    private Reply execute0(KeyInfo keyInfo) {
        if (keyInfo == null) {
            return IntegerReply.REPLY_NEGATIVE_2;
        }
        if (keyInfo.getExpireTime() <= 0) {
            return IntegerReply.REPLY_NEGATIVE_1;
        }
        long expireTime = keyInfo.getExpireTime();
        return new IntegerReply(expireTime / 1000);
    }
}
