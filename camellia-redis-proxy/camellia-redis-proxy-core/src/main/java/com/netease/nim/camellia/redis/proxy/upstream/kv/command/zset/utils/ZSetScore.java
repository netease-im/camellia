package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils;

import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * Created by caojiajun on 2024/5/14
 */
public class ZSetScore {

    private final double score;
    private final boolean excludeScore;

    private ZSetScore(double score, boolean excludeScore) {
        this.score = score;
        this.excludeScore = excludeScore;
    }

    public static ZSetScore fromBytes(byte[] bytes) {
        String str = Utils.bytesToString(bytes);
        if (str.equalsIgnoreCase("-inf")) {
            return new ZSetScore(Double.MIN_VALUE, false);
        } else if (str.equalsIgnoreCase("+inf")) {
            return new ZSetScore(Double.MAX_VALUE, false);
        } else if (str.startsWith("(")) {
            double score = Double.parseDouble(str.substring(1));
            return new ZSetScore(score, true);
        } else {
            double score = Double.parseDouble(str);
            return new ZSetScore(score, false);
        }
    }

    public double getScore() {
        return score;
    }

    public boolean isExcludeScore() {
        return excludeScore;
    }
}
