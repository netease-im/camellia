package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils;


import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;

/**
 * Created by caojiajun on 2024/5/15
 */
public class ZSetLexUtil {

    public static boolean checkLex(byte[] member, ZSetLex minLex, ZSetLex maxLex) {
        if (minLex.isMin() && maxLex.isMax()) {
            return true;
        }
        if (!maxLex.isMax()) {
            if (maxLex.isExcludeLex()) {
                if (BytesUtils.compare(member, maxLex.getLex()) >= 0) {
                    return false;
                }
            } else {
                if (BytesUtils.compare(member, maxLex.getLex()) > 0) {
                    return false;
                }
            }
        }
        if (!minLex.isMin()) {
            if (minLex.isExcludeLex()) {
                if (BytesUtils.compare(member, minLex.getLex()) <= 0) {
                    return false;
                }
            } else {
                if (BytesUtils.compare(member, minLex.getLex()) < 0) {
                    return false;
                }
            }
        }
        return true;
    }
}
