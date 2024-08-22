package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetLimit;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetScore;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetScoreUtils;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetTuple;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2024/8/13
 */
public abstract class ZRangeByScore0Commander extends ZRem0Commander {

    public ZRangeByScore0Commander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    protected final List<ZSetTuple> zrangeByScoreVersion0(KeyMeta keyMeta, byte[] key, ZSetScore minScore, ZSetScore maxScore, ZSetLimit limit, boolean withScores) {

        byte[] startKey = keyDesign.zsetMemberSubKey2(keyMeta, key, new byte[0], BytesUtils.toBytes(minScore.getScore()));
        byte[] endKey = BytesUtils.nextBytes(keyDesign.zsetMemberSubKey2(keyMeta, key, new byte[0], BytesUtils.toBytes(maxScore.getScore())));
        byte[] prefix = keyDesign.subKeyPrefix2(keyMeta, key);
        //
        int zsetMaxSize = kvConfig.zsetMaxSize();
        int batch = kvConfig.scanBatch();
        int count = 0;
        List<ZSetTuple> result = new ArrayList<>(limit.getCount() < 0 ? 16 : Math.min(limit.getCount(), 100));
        while (true) {
            if (limit.getCount() > 0) {
                batch = Math.min(kvConfig.scanBatch(), limit.getCount() - result.size());
            }
            List<KeyValue> list = kvClient.scanByStartEnd(startKey, endKey, prefix, batch, Sort.ASC, false);
            if (list.isEmpty()) {
                break;
            }
            for (KeyValue keyValue : list) {
                if (keyValue == null) {
                    continue;
                }
                startKey = keyValue.getKey();
                if (keyValue.getValue() == null) {
                    continue;
                }
                double score = keyDesign.decodeZSetScoreBySubKey2(keyValue.getKey(), key);
                boolean pass = ZSetScoreUtils.checkScore(score, minScore, maxScore);
                if (!pass) {
                    continue;
                }
                if (count >= limit.getOffset()) {
                    byte[] member = keyDesign.decodeZSetMemberBySubKey2(keyValue.getKey(), key);
                    ZSetTuple tuple;
                    if (withScores) {
                        tuple = new ZSetTuple(new BytesKey(member), score);
                    } else {
                        tuple = new ZSetTuple(new BytesKey(member), null);
                    }
                    result.add(tuple);
                    if (result.size() >= zsetMaxSize) {
                        ErrorLogCollector.collect(ZRangeByScore0Commander.class, "zset.size exceed " + zsetMaxSize + ", key = " + Utils.bytesToString(key));
                    }
                    if (limit.getCount() > 0 && result.size() >= limit.getCount()) {
                        break;
                    }
                }
                count ++;
            }
            if (list.size() < batch) {
                break;
            }
        }
        return result;
    }
}
