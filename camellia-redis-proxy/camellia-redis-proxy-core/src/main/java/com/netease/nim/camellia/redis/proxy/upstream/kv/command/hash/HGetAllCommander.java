package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HGETALL key
 * <p>
 * Created by caojiajun on 2024/4/7
 */
public class HGetAllCommander extends Commander {

    private static final byte[] script = ("local arg = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(arg) == 1 then\n" +
            "\tlocal ret = redis.call('hgetall', KEYS[1]);\n" +
            "\tredis.call('pexpire', KEYS[1], ARGV[1]);\n" +
            "\treturn {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

    public HGetAllCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HGETALL;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 2;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];

        //meta
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return MultiBulkReply.EMPTY;
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_1) {
            Map<BytesKey, byte[]> map = hgetallFromKv(keyMeta, key);
            if (map.isEmpty()) {
                return MultiBulkReply.EMPTY;
            }
            Reply[] replies = new Reply[map.size() * 2];
            int i = 0;
            for (Map.Entry<BytesKey, byte[]> entry : map.entrySet()) {
                replies[i] = new BulkReply(entry.getKey().getKey());
                replies[i + 1] = new BulkReply(entry.getValue());
                i += 2;
            }
            return new MultiBulkReply(replies);
        }

        //get cache first
        byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);
        {
            //cache
            Reply reply = sync(cacheRedisTemplate.sendLua(script, new byte[][]{cacheKey}, new byte[][]{hgetallCacheMillis()}));
            if (reply instanceof ErrorReply) {
                return reply;
            }
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                String type = Utils.bytesToString(((BulkReply) replies[0]).getRaw());
                if (type.equalsIgnoreCase("1")) {//cache hit
                    return replies[1];
                }
            }
        }

        //get from kv
        Map<BytesKey, byte[]> map = hgetallFromKv(keyMeta, key);

        if (map.isEmpty()) {
            return MultiBulkReply.EMPTY;
        }

        //build cache
        byte[][] args1 = new byte[map.size() * 2 + 2][];
        args1[0] = RedisCommand.HSET.raw();
        args1[1] = cacheKey;
        Reply[] replies = new Reply[map.size() * 2];
        int i = 0;
        for (Map.Entry<BytesKey, byte[]> entry : map.entrySet()) {
            replies[i] = new BulkReply(entry.getKey().getKey());
            replies[i + 1] = new BulkReply(entry.getValue());

            args1[i + 2] = entry.getKey().getKey();
            args1[i + 3] = entry.getValue();

            i += 2;
        }
        Command hsetCmd = new Command(args1);
        Command pexpireCmd = new Command(new byte[][]{RedisCommand.PEXPIRE.raw(), cacheKey, hgetallCacheMillis()});
        List<Command> commands = new ArrayList<>(2);
        commands.add(hsetCmd);
        commands.add(pexpireCmd);
        List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(commands));
        for (Reply reply : replyList) {
            if (reply instanceof ErrorReply) {
                return reply;
            }
        }
        return new MultiBulkReply(replies);
    }

    private Map<BytesKey, byte[]> hgetallFromKv(KeyMeta keyMeta, byte[] key) {
        Map<BytesKey, byte[]> map = new HashMap<>();
        byte[] startKey = keyStruct.hashFieldSubKey(keyMeta, key, new byte[0]);
        byte[] prefix = startKey;
        int limit = kvConfig.scanBatch();
        int hashMaxSize = kvConfig.hashMaxSize();
        while (true) {
            List<KeyValue> scan = kvClient.scan(startKey, prefix, limit, Sort.ASC, false);
            if (scan.isEmpty()) {
                break;
            }
            for (KeyValue keyValue : scan) {
                byte[] field = keyStruct.decodeHashFieldBySubKey(keyValue.getKey(), key);
                map.put(new BytesKey(field), keyValue.getValue());
                startKey = keyValue.getKey();
                if (map.size() >= hashMaxSize) {
                    break;
                }
            }
            if (scan.size() < limit) {
                break;
            }
        }
        return map;
    }

    private byte[] hgetallCacheMillis() {
        return Utils.stringToBytes(String.valueOf(cacheConfig.hgetallCacheMillis()));
    }
}
