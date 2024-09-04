package com.netease.nim.camellia.redis.proxy.upstream.kv.command.string;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * SET key value [NX | XX] [GET] [EX seconds | PX milliseconds | EXAT unix-time-seconds | PXAT unix-time-milliseconds | KEEPTTL]
 * Created by caojiajun on 2024/4/11
 */
public class SetCommander extends Commander {

    private static final int nx = 1;
    private static final int xx = 2;

    public SetCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
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
    protected Reply execute(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
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

        KeyMeta keyMeta = null;
        if (nxxx == nx) {
            if (get) {
                ErrorLogCollector.collect(SetCommander.class, "set command syntax error, nx && get");
                return ErrorReply.SYNTAX_ERROR;
            }
            keyMeta = keyMetaServer.getKeyMeta(slot, key);
            if (keyMeta != null) {
                return BulkReply.NIL_REPLY;
            }
        } else if (nxxx == xx) {
            keyMeta = keyMetaServer.getKeyMeta(slot, key);
            if (keyMeta == null) {
                return BulkReply.NIL_REPLY;
            }
        }
        if (keepTtl && keyMeta == null) {
            keyMeta = keyMetaServer.getKeyMeta(slot, key);
        }
        byte[] oldValue = keyMeta == null ? null : keyMeta.getExtra();
        if (keepTtl) {
            expireTime = keyMeta == null ? -1 : keyMeta.getExpireTime();
            keyMeta = new KeyMeta(EncodeVersion.version_0, KeyType.string, System.currentTimeMillis(), expireTime, value);
        } else if (expireTime > 0) {
            keyMeta = new KeyMeta(EncodeVersion.version_0, KeyType.string, System.currentTimeMillis(), expireTime, value);
        } else {
            keyMeta = new KeyMeta(EncodeVersion.version_0, KeyType.string, System.currentTimeMillis(), -1 , value);
        }
        keyMetaServer.createOrUpdateKeyMeta(slot, key, keyMeta);
        if (get) {
            return new BulkReply(oldValue);
        } else {
            return StatusReply.OK;
        }
    }
}
