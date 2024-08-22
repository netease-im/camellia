package com.netease.nim.camellia.redis.proxy.upstream.kv.command.db;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.db.utils.ScanParam;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.db.utils.ScanParamUtil;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


/**
 * SCAN cursor [MATCH pattern] [COUNT count] [TYPE type]
 * <p>
 * Created by caojiajun on 2024/8/21
 */
public class ScanCommander extends Commander {

    private final ConcurrentLinkedHashMap<Integer, byte[]> cursorCache = new ConcurrentLinkedHashMap.Builder<Integer, byte[]>()
            .initialCapacity(10000)
            .maximumWeightedCapacity(10000)
            .build();

    private static final BulkReply completeCursor = new BulkReply(Utils.stringToBytes("0"));

    public ScanCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
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
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        int cursor = (int)Utils.bytesToNum(objects[1]);
        byte[] startKey;
        if (cursor != 0) {
            startKey = cursorCache.get(cursor);
            if (startKey == null) {
                return ErrorReply.SYNTAX_ERROR;
            }
        } else {
            startKey = keyDesign.getMetaPrefix();
        }

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
            if (loop >= 2) {
                break;
            }
            loop ++;
            List<KeyValue> scan = kvClient.scanByPrefix(startKey, metaPrefix, limit, Sort.ASC, false);
            if (scan.isEmpty()) {
                complete = true;
                break;
            }
            for (KeyValue keyValue : scan) {
                startKey = keyValue.getKey();
                KeyMeta keyMeta;
                try {
                    keyMeta = KeyMeta.fromBytes(keyValue.getValue());
                } catch (Exception e) {
                    kvClient.delete(keyValue.getKey());
                    continue;
                }
                if (keyMeta == null) {
                    continue;
                }
                byte[] key;
                try {
                    key = keyDesign.decodeKeyByMetaKey(startKey);
                } catch (Exception e) {
                    kvClient.delete(keyValue.getKey());
                    continue;
                }
                if (keyMeta.isExpire()) {
                    if (key != null) {
                        gcExecutor.submitSubKeyDeleteTask(key, keyMeta);
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
            if (result.size() == limit) {
                break;
            }
        }
        BulkReply nextCursor;
        if (complete) {
            nextCursor = completeCursor;
        } else {
            int nextCursorValue = toNumberCursor(startKey);
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

    private int toNumberCursor(byte[] startKey) {
        int cursor = 0;
        for (byte element : startKey) {
            cursor = 31 * cursor + element;
        }
        cursor = Math.abs(cursor);
        while (true) {
            byte[] oldValue = cursorCache.get(cursor);
            if (oldValue == null) {
                break;
            }
            cursor += ThreadLocalRandom.current().nextInt(1000);
            cursor = Math.abs(cursor);
            oldValue = cursorCache.putIfAbsent(cursor, startKey);
            if (oldValue == null) {
                break;
            }
        }
        return cursor;
    }


}
