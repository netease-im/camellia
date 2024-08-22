package com.netease.nim.camellia.redis.proxy.upstream.kv.command.db.utils;

import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.regex.Pattern;

/**
 * Created by caojiajun on 2024/8/22
 */
public class ScanParam {

    protected int count = 10;
    protected String type;
    protected Pattern pattern;

    public boolean match(byte[] key, KeyMeta keyMeta) {
        if (type != null) {
            if (!type.equalsIgnoreCase(keyMeta.getKeyType().name())) {
                return false;
            }
        }
        if (pattern != null) {
            boolean matches = pattern.matcher(Utils.bytesToString(key)).matches();
            if (!matches) {
                return false;
            }
        }
        return true;
    }

    public int getCount() {
        return count;
    }
}

