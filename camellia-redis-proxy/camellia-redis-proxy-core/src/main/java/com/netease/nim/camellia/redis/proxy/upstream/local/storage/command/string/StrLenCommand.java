package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.string;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.CommandConfig;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.ICommand;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums.DataType;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * STRLEN key
 * <p>
 * Created by caojiajun on 2025/1/10
 */
public class StrLenCommand extends ICommand {

    public StrLenCommand(CommandConfig commandConfig) {
        super(commandConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.STRLEN;
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
        if (keyInfo == null) {
            return IntegerReply.REPLY_0;
        }
        if (keyInfo.getDataType() != DataType.string) {
            return ErrorReply.WRONG_TYPE;
        }
        byte[] value;
        if (keyInfo.containsExtra()) {
            value = keyInfo.getExtra();
        } else {
            ValueWrapper<byte[]> valueWrapper = stringReadWrite.getForRunToCompletion(slot, keyInfo);
            if (valueWrapper == null) {
                return null;
            }
            value = valueWrapper.get();
        }
        if (value == null) {
            return IntegerReply.REPLY_0;
        }
        return IntegerReply.parse(Utils.bytesToString(value).length());
    }

    @Override
    protected Reply execute(short slot, Command command) throws Exception {
        byte[][] objects = command.getObjects();
        Key key = new Key(objects[1]);
        KeyInfo keyInfo = keyReadWrite.get(slot, key);
        if (keyInfo == null) {
            return IntegerReply.REPLY_0;
        }
        if (keyInfo.getDataType() != DataType.string) {
            return ErrorReply.WRONG_TYPE;
        }
        byte[] value;
        if (keyInfo.containsExtra()) {
            value = keyInfo.getExtra();
        } else {
            value = stringReadWrite.get(slot, keyInfo);
        }
        if (value == null) {
            return IntegerReply.REPLY_0;
        }
        return IntegerReply.parse(Utils.bytesToString(value).length());
    }

}
