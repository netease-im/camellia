package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.command.string;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.command.CommandOnEmbeddedStorage;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.enums.DataType;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.enums.FlushResult;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.string.SetCommander;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

/**
 * SET key value [NX | XX] [GET] [EX seconds | PX milliseconds | EXAT unix-time-seconds | PXAT unix-time-milliseconds | KEEPTTL]
 * Created by caojiajun on 2025/1/3
 */
public class Set extends CommandOnEmbeddedStorage {

    private static final int nx = 1;
    private static final int xx = 2;

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SET;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length >= 3;
    }

    @Override
    protected Reply execute(short slot, Command command) throws Exception {
        byte[][] objects = command.getObjects();
        BytesKey key = new BytesKey(objects[1]);
        byte[] value = objects[2];
        int nxxx = -1;
        long expireTime = -1;
        boolean get = false;
        boolean keepTtl = false;
        if (objects.length > 3) {
            for (int i=3; i<objects.length; i++) {
                String param = Utils.bytesToString(objects[i]);
                if (param.equalsIgnoreCase("NX")) {
                    nxxx = nx;
                } else if (param.equalsIgnoreCase("XX")) {
                    nxxx = xx;
                } else if (param.equalsIgnoreCase("GET")) {
                    get = true;
                } else if (param.equalsIgnoreCase("EX")) {
                    long seconds = Utils.bytesToNum(objects[i + 1]);
                    i ++;
                    expireTime = System.currentTimeMillis() + seconds * 1000L;
                } else if (param.equalsIgnoreCase("PX")) {
                    long millis = Utils.bytesToNum(objects[i + 1]);
                    i ++;
                    expireTime = System.currentTimeMillis() + millis;
                } else if (param.equalsIgnoreCase("EXAT")) {
                    long secondsAt = Utils.bytesToNum(objects[i + 1]);
                    i ++;
                    expireTime = secondsAt * 1000L;
                } else if (param.equalsIgnoreCase("PXAT")) {
                    long millisAt = Utils.bytesToNum(objects[i + 1]);
                    i ++;
                    expireTime = millisAt;
                } else if (param.equalsIgnoreCase("KEEPTTL")) {
                    keepTtl = true;
                }
            }
        }

        if (expireTime > 0 && keepTtl) {
            ErrorLogCollector.collect(SetCommander.class, "set command syntax error, expireTime > 0 && keepTtl");
            return ErrorReply.SYNTAX_ERROR;
        }

        KeyInfo keyInfo = null;
        if (nxxx == nx) {
            if (get) {
                ErrorLogCollector.collect(SetCommander.class, "set command syntax error, nx && get");
                return ErrorReply.SYNTAX_ERROR;
            }
            keyInfo = keyReadWrite.get(slot, key);
            if (keyInfo != null) {
                return BulkReply.NIL_REPLY;
            }
        } else if (nxxx == xx) {
            keyInfo = keyReadWrite.get(slot, key);
            if (keyInfo == null) {
                return BulkReply.NIL_REPLY;
            }
        }
        if (keepTtl && keyInfo == null) {
            keyInfo = keyReadWrite.get(slot, key);
        }
        byte[] oldValue = keyInfo == null ? null : keyInfo.getExtra();
        if (keepTtl) {
            expireTime = keyInfo == null ? -1 : keyInfo.getExpireTime();
            keyInfo = new KeyInfo(DataType.string);
            keyInfo.setExpireTime(expireTime);
        } else if (expireTime > 0) {
            keyInfo = new KeyInfo(DataType.string);
            keyInfo.setExpireTime(expireTime);
        } else {
            keyInfo = new KeyInfo(DataType.string);
        }

        walGroup.append(slot, command);

        if (key.getKey().length + value.length <= 128) {
            keyInfo.setExtra(value);
        } else {
            keyInfo.setExtra(null);
            stringReadWrite.put(slot, keyInfo, value);
        }
        keyReadWrite.put(slot, keyInfo);

        checkAndFlush(slot);

        if (get) {
            return new BulkReply(oldValue);
        } else {
            return StatusReply.OK;
        }
    }
}
