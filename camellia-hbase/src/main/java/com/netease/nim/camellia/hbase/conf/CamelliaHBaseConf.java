package com.netease.nim.camellia.hbase.conf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2020/3/20.
 */
public class CamelliaHBaseConf {

    private Map<String, String> properties = new HashMap<>();

    public CamelliaHBaseConf() {
    }

    public CamelliaHBaseConf(Map<String, String> properties) {
        this.properties = properties;
    }

    public void addConf(String key, String value) {
        properties.put(key, value);
    }

    public void removeConf(String key) {
        properties.remove(key);
    }

    public String getConf(String key) {
        return properties.get(key);
    }

    public Map<String, String> getConfMap() {
        return Collections.unmodifiableMap(properties);
    }
}
