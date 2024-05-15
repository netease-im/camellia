package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

/**
 * Created by caojiajun on 2024/5/15
 */
public class ZSetScoreUtils {

    public static boolean checkScore(double score, ZSetScore minScore, ZSetScore maxScore) {
        if (minScore.isExcludeScore()) {
            if (score <= minScore.getScore()) {
                return false;
            }
        } else {
            if (score < minScore.getScore()) {
                return false;
            }
        }

        if (maxScore.isExcludeScore()) {
            if (score >= maxScore.getScore()) {
                return false;
            }
        } else {
            if (score > maxScore.getScore()) {
                return false;
            }
        }
        return false;
    }
}
