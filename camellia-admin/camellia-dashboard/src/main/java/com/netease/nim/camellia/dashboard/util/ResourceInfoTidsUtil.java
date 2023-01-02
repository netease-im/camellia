package com.netease.nim.camellia.dashboard.util;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2019/11/20.
 */
public class ResourceInfoTidsUtil {

    public static Set<Long> parseTids(String tids) {
        if (tids == null || tids.length() == 0) return new HashSet<>();
        Set<Long> set = new HashSet<>();
        String[] split = tids.split(",");
        for (String tidStr : split) {
            long tid = Long.parseLong(tidStr);
            set.add(tid);
        }
        return set;
    }

    public static String toString(Set<Long> tids) {
        if (tids == null || tids.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (Long tid : tids) {
            builder.append(tid).append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }
}
