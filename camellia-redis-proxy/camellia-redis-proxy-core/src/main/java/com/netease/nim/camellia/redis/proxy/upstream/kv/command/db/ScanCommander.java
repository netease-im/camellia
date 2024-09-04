package com.netease.nim.camellia.redis.proxy.upstream.kv.command.db;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.db.utils.ScanParam;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.db.utils.ScanParamUtil;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


/**
 * SCAN cursor [MATCH pattern] [COUNT count] [TYPE type]
 * <p>
 * Created by caojiajun on 2024/8/21
 */
public class ScanCommander extends Commander {

    private static final Logger logger = LoggerFactory.getLogger(ScanCommander.class);

    private static final BulkReply completeCursor = new BulkReply(Utils.stringToBytes("0"));

    private final String namespace;
    private final ConcurrentLinkedHashMap<Integer, Pair<byte[], Integer>> cursorCache;
    private int capacity;
    private int maxLoop;

    public ScanCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
        this.namespace = commanderConfig.getCacheConfig().getNamespace();
        this.capacity = RedisKvConf.getInt(namespace, "kv.scan.cursor.cache.capacity", 10000);
        this.maxLoop = RedisKvConf.getInt(namespace, "kv.scan.max.loop", 10);
        this.cursorCache = new ConcurrentLinkedHashMap.Builder<Integer, Pair<byte[], Integer>>()
                .initialCapacity(capacity)
                .maximumWeightedCapacity(capacity)
                .build();
        logger.info("scan cursor cache init, namespace = {}, capacity = {}, max.loop = {}", namespace, capacity, maxLoop);
        ProxyDynamicConf.registerCallback(this::rebuild);
    }

    private void rebuild() {
        int capacity = RedisKvConf.getInt(namespace, "kv.scan.cursor.cache.capacity", 10000);
        if (capacity != this.capacity) {
            cursorCache.setCapacity(capacity);
            this.capacity = capacity;
            logger.info("scan cursor cache update, namespace = {}, capacity = {}", namespace, capacity);
        }
        int maxLoop = RedisKvConf.getInt(namespace, "kv.scan.max.loop", 10);
        if (maxLoop != this.maxLoop) {
            this.maxLoop = maxLoop;
            logger.info("scan max loop update, namespace = {}, max.loop = {}", namespace, maxLoop);
        }
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SCAN;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length >= 2 && objects.length % 2 == 0;
    }

    @Override
    protected Reply execute(int slot, Command command) {
        byte[][] objects = command.getObjects();
        int cursor = (int)Utils.bytesToNum(objects[1]);

        Pair<byte[], Integer> pair;
        if (cursor != 0) {
            pair = cursorCache.get(cursor);
            if (pair == null) {
                ErrorLogCollector.collect(ScanCommander.class, "scan cursor not found in cache");
                return ErrorReply.SYNTAX_ERROR;
            }
        } else {
            pair = new Pair<>(keyDesign.getMetaPrefix(), 0);
        }
        byte[] startKey = pair.getFirst();
        slot = pair.getSecond();

        ScanParam param = new ScanParam();

        ErrorReply reply = ScanParamUtil.parseScanParam(param, objects);
        if (reply != null) {
            return reply;
        }

        int limit = param.getCount();
        byte[] metaPrefix = keyDesign.getMetaPrefix();
        int loop = 0;
        List<byte[]> result = new ArrayList<>();
        boolean complete = false;
        while (true) {
            if (loop >= maxLoop) {
                break;
            }
            loop ++;
            List<KeyValue> scan = kvClient.scanByPrefix(slot, startKey, metaPrefix, limit, Sort.ASC, false);
            if (scan.isEmpty()) {
                if (slot == RedisClusterCRC16Utils.SLOT_SIZE - 1) {
                    complete = true;
                    break;
                } else {
                    slot++;
                    startKey = metaPrefix;
                    continue;
                }
            }
            for (KeyValue keyValue : scan) {
                startKey = keyValue.getKey();
                KeyMeta keyMeta;
                try {
                    keyMeta = KeyMeta.fromBytes(keyValue.getValue());
                } catch (Exception e) {
                    kvClient.delete(slot, keyValue.getKey());
                    continue;
                }
                if (keyMeta == null) {
                    continue;
                }
                byte[] key;
                try {
                    key = keyDesign.decodeKeyByMetaKey(startKey);
                } catch (Exception e) {
                    kvClient.delete(slot, keyValue.getKey());
                    continue;
                }
                if (keyMeta.isExpire()) {
                    if (key != null) {
                        gcExecutor.submitSubKeyDeleteTask(slot, key, keyMeta);
                    }
                    continue;
                }
                if (param.match(key, keyMeta)) {
                    result.add(key);
                    if (result.size() >= limit) {
                        break;
                    }
                }
            }
            if (result.size() >= limit) {
                break;
            }
        }
        BulkReply nextCursor;
        if (complete) {
            nextCursor = completeCursor;
        } else {
            int nextCursorValue = toNumberCursor(new Pair<>(startKey, slot));
            nextCursor = new BulkReply(Utils.stringToBytes(String.valueOf(nextCursorValue)));
        }
        Reply[] replies = new Reply[result.size()];
        for (int i=0; i<result.size(); i++) {
            byte[] key = result.get(i);
            replies[i] = new BulkReply(key);
        }
        MultiBulkReply multiBulkReply = new MultiBulkReply(replies);
        return new MultiBulkReply(new Reply[] {nextCursor, multiBulkReply});
    }

    private int toNumberCursor(Pair<byte[], Integer> pair) {
        int cursor = 0;
        for (byte element : pair.getFirst()) {
            cursor = 31 * cursor + element;
        }
        cursor = Math.abs(cursor);
        while (true) {
            Pair<byte[], Integer> oldValue = cursorCache.putIfAbsent(cursor, pair);
            if (oldValue == null) {
                break;
            }
            cursor += ThreadLocalRandom.current().nextInt(1000);
            cursor = Math.abs(cursor);
        }
        return cursor;
    }


}
