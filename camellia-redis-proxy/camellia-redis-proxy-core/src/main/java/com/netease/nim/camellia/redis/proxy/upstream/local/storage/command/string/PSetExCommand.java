package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.string;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.CommandConfig;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.ICommand;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums.DataType;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import static com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants._1024k;

/**
 * PSETEX key milliseconds value
 * <p>
 * Created by caojiajun on 2025/1/10
 */
public class PSetExCommand extends ICommand {

    public PSetExCommand(CommandConfig commandConfig) {
        super(commandConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.PSETEX;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 4;
    }

    @Override
    protected Reply execute(short slot, Command command) throws Exception {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        long millis = Utils.bytesToNum(objects[2]);
        byte[] value = objects[3];

        if (value.length > _1024k) {
            return ErrorReply.VALUE_TOO_LONG;
        }

        long expireTime = System.currentTimeMillis() + millis;
        KeyInfo keyInfo = new KeyInfo(DataType.string, key);
        keyInfo.setExpireTime(expireTime);

        if (key.length + value.length <= 128) {
            keyInfo.setExtra(value);
        } else {
            keyInfo.setExtra(null);
            stringReadWrite.put(slot, keyInfo, value);
        }
        keyReadWrite.put(slot, keyInfo);

        return StatusReply.OK;
    }
}
