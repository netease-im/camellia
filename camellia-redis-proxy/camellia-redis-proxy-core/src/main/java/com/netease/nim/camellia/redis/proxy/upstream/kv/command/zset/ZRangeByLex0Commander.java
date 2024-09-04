package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetLex;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetLexUtil;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetLimit;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetTuple;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2024/8/13
 */
public abstract class ZRangeByLex0Commander extends ZRem0Commander {

    public ZRangeByLex0Commander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    protected final List<ZSetTuple> zrangeByLexVersion0(int slot, KeyMeta keyMeta, byte[] key, ZSetLex minLex, ZSetLex maxLex, ZSetLimit limit, boolean withScores) {
        byte[] startKey;
        if (minLex.isMin()) {
            startKey = keyDesign.zsetMemberSubKey1(keyMeta, key, new byte[0]);
        } else {
            startKey = keyDesign.zsetMemberSubKey1(keyMeta, key, minLex.getLex());
        }
        byte[] endKey;
        if (maxLex.isMax()) {
            endKey = BytesUtils.nextBytes(keyDesign.zsetMemberSubKey1(keyMeta, key, new byte[0]));
        } else {
            if (maxLex.isExcludeLex()) {
                endKey = keyDesign.zsetMemberSubKey1(keyMeta, key, maxLex.getLex());
            } else {
                endKey = BytesUtils.nextBytes(keyDesign.zsetMemberSubKey1(keyMeta, key, maxLex.getLex()));
            }
        }
        byte[] prefix = keyDesign.subKeyPrefix(keyMeta, key);
        //
        List<ZSetTuple> result = new ArrayList<>();
        int scanBatch = kvConfig.scanBatch();
        int count = 0;
        int loop = 0;
        boolean includeStartKey;
        while (true) {
            if (loop == 0) {
                includeStartKey = !minLex.isExcludeLex();
            } else {
                includeStartKey = false;
            }
            List<KeyValue> scan = kvClient.scanByStartEnd(slot, startKey, endKey, prefix, scanBatch, Sort.ASC, includeStartKey);
            loop ++;
            if (scan.isEmpty()) {
                return result;
            }
            for (KeyValue keyValue : scan) {
                if (keyValue == null || keyValue.getValue() == null) {
                    continue;
                }
                startKey = keyValue.getKey();
                byte[] member = keyDesign.decodeZSetMemberBySubKey1(keyValue.getKey(), key);
                boolean pass = ZSetLexUtil.checkLex(member, minLex, maxLex);
                if (!pass) {
                    continue;
                }
                if (count >= limit.getOffset()) {
                    if (withScores) {
                        double score = Utils.bytesToDouble(keyValue.getValue());
                        result.add(new ZSetTuple(new BytesKey(member), score));
                    } else {
                        result.add(new ZSetTuple(new BytesKey(member), null));
                    }
                    if (limit.getCount() > 0 && result.size() >= limit.getCount()) {
                        return result;
                    }
                }
                count++;
            }
            if (scan.size() < scanBatch) {
                return result;
            }
        }
    }
}
