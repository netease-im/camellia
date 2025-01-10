package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.string;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.CommandConfig;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.ICommand;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums.DataType;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;

/**
 * GET key
 * <p>
 * Created by caojiajun on 2025/1/3
 */
public class GetCommand extends ICommand {

    public GetCommand(CommandConfig commandConfig) {
        super(commandConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.GET;
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
            return BulkReply.NIL_REPLY;
        }
        if (keyInfo.containsExtra()) {
            return new BulkReply(keyInfo.getExtra());
        }
        ValueWrapper<byte[]> valueWrapper = stringReadWrite.getForRunToCompletion(slot, keyInfo);
        if (valueWrapper == null) {
            return null;
        }
        byte[] bytes = valueWrapper.get();
        if (bytes == null) {
            return BulkReply.NIL_REPLY;
        }
        return new BulkReply(bytes);
    }

    @Override
    protected Reply execute(short slot, Command command) throws Exception {
        byte[][] objects = command.getObjects();
        Key key = new Key(objects[1]);
        KeyInfo keyInfo = keyReadWrite.get(slot, key);
        if (keyInfo == null) {
            return BulkReply.NIL_REPLY;
        }
        if (keyInfo.getDataType() != DataType.string) {
            return ErrorReply.WRONG_TYPE;
        }
        if (keyInfo.containsExtra()) {
            return new BulkReply(keyInfo.getExtra());
        }
        byte[] bytes = stringReadWrite.get(slot, keyInfo);
        if (bytes == null) {
            return BulkReply.NIL_REPLY;
        }
        return new BulkReply(bytes);
    }
}
