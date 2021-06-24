package com.netease.nim.camellia.redis.proxy.hbase.util;

import com.netease.nim.camellia.core.util.MD5Util;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by caojiajun on 2020/12/21
 */
public class RedisHBaseUtils {

    public static final byte[] CF_D = Bytes.toBytes("d");
    public static final byte[] COL_DATA = Bytes.toBytes("data");

    public static byte[] redisKey(byte[] key) {
        return Bytes.add(Utils.stringToBytes(RedisHBaseConfiguration.redisKeyPrefix()), key);
    }

    public static <T> List<List<T>> split(List<T> list, int maxSplit) {
        if (list == null) return null;
        List<List<T>> ret = new ArrayList<>();
        if (list.size() <= maxSplit) {
            ret.add(list);
            return ret;
        }
        List<T> subList = new ArrayList<>();
        for (T t : list) {
            subList.add(t);
            if (subList.size() >= maxSplit) {
                ret.add(subList);
                subList = new ArrayList<>();
            }
        }
        if (!subList.isEmpty()) {
            ret.add(subList);
        }
        return ret;
    }

    //32字节
    public static byte[] buildMemberRefKey(byte[] key, byte[] member) {
        byte[] part1 = MD5Util.md5(key);
        byte[] part2 = MD5Util.md5(member);
        return Bytes.add(part1, part2);
    }

    public static String hbaseTableName() {
        return RedisHBaseConfiguration.hbaseTableName();
    }

    public static boolean isMemberRefKey(byte[] key, byte[] member) {
        if (member.length != 32) return false;
        byte[] part1 = MD5Util.md5(key);
        if (member.length < part1.length) return false;
        for (int i=0; i<part1.length; i++) {
            if (part1[i] != member[i]) {
                return false;
            }
        }
        return true;
    }

    public static byte[] parseOriginalValue(Result result) {
        if (result == null) return null;
        return result.getValue(CF_D, COL_DATA);
    }

    public static final int ZSET_MEMBER_REF_KEY_THRESHOLD_MIN = 32;//阈值最小值是32
    public static int zsetMemberRefKeyThreshold() {
        int threshold = RedisHBaseConfiguration.zsetMemberRefKeyThreshold();
        return Math.max(threshold, ZSET_MEMBER_REF_KEY_THRESHOLD_MIN);
    }

    public static int zsetMemberRefKeyExpireSeconds() {
        return RedisHBaseConfiguration.zsetMemberRefKeyExpireSeconds();
    }
}
