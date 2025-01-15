package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetRank;
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
public abstract class ZRangeByRank0Commander extends ZRem0Commander {

    public ZRangeByRank0Commander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    protected final List<ZSetTuple> zrangeByRankVersion0(int slot, KeyMeta keyMeta, byte[] key, int start, int stop, boolean withScores) {

        int size = BytesUtils.toInt(keyMeta.getExtra());
        ZSetRank zSetRank = new ZSetRank(start, stop, size);
        if (zSetRank.isEmptyRank()) {
            return new ArrayList<>();
        }
        start = zSetRank.getStart();
        stop = zSetRank.getStop();

        byte[] startKey = keyDesign.zsetMemberSubKey2(keyMeta, key, new byte[0], new byte[0]);
        byte[] prefix = startKey;
        int targetSize = stop - start + 1;

        List<ZSetTuple> result = new ArrayList<>(targetSize);
        int scanBatch = kvConfig.scanBatch();
        int count = 0;
        while (true) {
            int limit = Math.min(stop - count + 1, scanBatch);
            List<KeyValue> scan = kvClient.scanByPrefix(slot, startKey, prefix, limit, Sort.ASC, false);
            if (scan.isEmpty()) {
                return result;
            }
            for (KeyValue keyValue : scan) {
                if (keyValue == null || keyValue.getValue() == null) {
                    continue;
                }
                startKey = keyValue.getKey();
                if (count >= start) {
                    byte[] member = keyDesign.decodeZSetMemberBySubKey2(keyValue.getKey(), key);
                    if (withScores) {
                        double score = keyDesign.decodeZSetScoreBySubKey2(keyValue.getKey(), key);
                        result.add(new ZSetTuple(new BytesKey(member), score));
                    } else {
                        result.add(new ZSetTuple(new BytesKey(member), null));
                    }
                }
                if (count >= stop) {
                    return result;
                }
                count++;
            }
            if (scan.size() < limit) {
                return result;
            }
        }
    }
}
