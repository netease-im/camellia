package com.netease.nim.camellia.tools.http;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;


public class ParamBuilder {
    private final Map<String, String> map = new HashMap<>();

    public ParamBuilder() {
    }

    public ParamBuilder addParam(Object key, Object value) {
        map.put(key.toString(), value.toString());
        return this;
    }

    public String build() {
        try {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                builder.append(URLEncoder.encode(entry.getKey(), "utf-8")).append("=")
                        .append(URLEncoder.encode(entry.getValue(), "utf-8")).append("&");
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            return builder.toString();
        } catch (Exception e) {
            throw new HttpException(e);
        }
    }
}

