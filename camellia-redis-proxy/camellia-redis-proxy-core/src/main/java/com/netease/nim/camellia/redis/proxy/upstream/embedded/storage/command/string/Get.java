package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.command.string;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.command.CommandOnEmbeddedStorage;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.enums.DataType;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.ValueLocation;
import com.netease.nim.camellia.tools.utils.BytesKey;

/**
 * GET key
 * <p>
 * Created by caojiajun on 2025/1/3
 */
public class Get extends CommandOnEmbeddedStorage {

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
    protected Reply execute(short slot, Command command) throws Exception {
        byte[][] objects = command.getObjects();
        BytesKey key = new BytesKey(objects[1]);
        KeyInfo keyInfo = keyReadWrite.get(slot, key);
        return execute0(keyInfo);
    }

    private Reply execute0(KeyInfo keyInfo) {
        if (keyInfo == null) {
            return BulkReply.NIL_REPLY;
        }
        if (keyInfo.getDataType() != DataType.string) {
            return ErrorReply.WRONG_TYPE;
        }
        if (keyInfo.containsExtra()) {
            return new BulkReply(keyInfo.getExtra());
        }
        ValueLocation valueLocation = keyInfo.getValueLocation();
        byte[] bytes = stringReadWrite.get(valueLocation);
        if (bytes == null) {
            return BulkReply.NIL_REPLY;
        }
        return new BulkReply(bytes);
    }
}
