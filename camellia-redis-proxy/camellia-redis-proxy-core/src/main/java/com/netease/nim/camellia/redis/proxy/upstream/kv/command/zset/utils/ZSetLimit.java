package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils;

import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * Created by caojiajun on 2024/5/14
 */
public class ZSetLimit {

    public static final ZSetLimit NO_LIMIT = new ZSetLimit(0, -1);

    private final int offset;
    private final int count;

    private ZSetLimit(int offset, int count) {
        this.offset = offset;
        this.count = count;
    }

    public static ZSetLimit fromBytes(byte[][] objects, int index) {
        for (int i=index; i<objects.length; i++) {
            String arg = Utils.bytesToString(objects[i]);
            if (arg.equalsIgnoreCase(RedisKeyword.LIMIT.name())) {
                int offset = (int) Utils.bytesToNum(objects[i + 1]);
                int count = (int) Utils.bytesToNum(objects[i + 2]);
                return new ZSetLimit(offset, count);
            }
        }
        return new ZSetLimit(0, -1);
    }

    public int getOffset() {
        return offset;
    }

    public int getCount() {
        return count;
    }
}
