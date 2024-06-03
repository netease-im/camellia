package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * Created by caojiajun on 2024/5/31
 */
public class ZSetWithScoresUtils {

    public static boolean isWithScores(byte[][] objects, int startIndex) {
        boolean withScores = false;
        for (int i=startIndex; i<objects.length; i++) {
            withScores = Utils.bytesToString(objects[i]).equalsIgnoreCase(RedisKeyword.WITHSCORES.name());
            if (withScores) {
                break;
            }
        }
        return withScores;
    }
}
