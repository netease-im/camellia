package com.netease.nim.camellia.hot.key.server.monitor;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by caojiajun on 2023/5/11
 */
public class StatsJsonConverter {

    public static String converter(HotKeyServerStats serverStats) {
        return new JSONObject().toJSONString();
    }
}
