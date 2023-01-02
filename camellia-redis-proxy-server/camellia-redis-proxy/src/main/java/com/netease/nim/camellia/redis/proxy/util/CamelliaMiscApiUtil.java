package com.netease.nim.camellia.redis.proxy.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.core.api.CamelliaMiscApi;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

import java.util.HashMap;
import java.util.Map;

/**
 * @author anhdt9
 * @since 22/12/2022
 */
public class CamelliaMiscApiUtil {
    private CamelliaMiscApiUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static CamelliaMiscApi initFromDynamicConfig() {
        String url = ProxyDynamicConf.getString("camellia.dashboard.url", null);
        int connectTimeoutMillis = ProxyDynamicConf.getInt("camellia.dashboard.connectTimeoutMillis", 10000);
        int readTimeoutMillis = ProxyDynamicConf.getInt("camellia.dashboard.readTimeoutMillis", 60000);
        Map<String, String> headerMap = new HashMap<>();
        String string = ProxyDynamicConf.getString("camellia.dashboard.headerMap", "{}");
        if (string != null && string.trim().length() > 0) {
            JSONObject json = JSON.parseObject(string);
            for (Map.Entry<String, Object> entry : json.entrySet()) {
                headerMap.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return CamelliaApiUtil.initMiscApi(url, connectTimeoutMillis, readTimeoutMillis, headerMap);
    }
}
