package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.string;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.CommandConfig;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.ICommand;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums.DataType;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.string.SetCommander;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal.StringWalEntry;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal.WalWriteResult;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants._1024k;

/**
 * SET key value [NX | XX] [GET] [EX seconds | PX milliseconds | EXAT unix-time-seconds | PXAT unix-time-milliseconds | KEEPTTL]
 * Created by caojiajun on 2025/1/3
 */
public class SetCommand extends ICommand {

    private static final int nx = 1;
    private static final int xx = 2;

    public SetCommand(CommandConfig commandConfig) {
        super(commandConfig);
    }

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
        Key key = new Key(objects[1]);
        byte[] value = objects[2];
        if (value.length > _1024k) {
            return ErrorReply.VALUE_TOO_LONG;
        }
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
            keyInfo = new KeyInfo(DataType.string, key.key());
            keyInfo.setExpireTime(expireTime);
        } else if (expireTime > 0) {
            keyInfo = new KeyInfo(DataType.string, key.key());
            keyInfo.setExpireTime(expireTime);
        } else {
            keyInfo = new KeyInfo(DataType.string, key.key());
        }

        byte[] bigValue = null;
        if (key.key().length + value.length <= 128) {
            keyInfo.setExtra(value);
        } else {
            keyInfo.setExtra(null);
            bigValue = value;
        }

        //写入wal
        StringWalEntry walEntry = new StringWalEntry(keyInfo, bigValue);
        CompletableFuture<WalWriteResult> future = wal.append(slot, walEntry);
        WalWriteResult result = future.get(30, TimeUnit.SECONDS);
        if (result != WalWriteResult.success) {
            return ErrorReply.INTERNAL_ERROR;
        }

        //写入memtable
        if (bigValue != null) {
            stringReadWrite.put(slot, keyInfo, value);
        }
        keyReadWrite.put(slot, keyInfo);

        //响应
        if (get) {
            return new BulkReply(oldValue);
        } else {
            return StatusReply.OK;
        }
    }
}
