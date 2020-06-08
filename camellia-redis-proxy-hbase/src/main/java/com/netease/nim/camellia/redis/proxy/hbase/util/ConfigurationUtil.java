package com.netease.nim.camellia.redis.proxy.hbase.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigurationUtil {

    public static Map<String, String> propertiesToMap(Properties properties) {
        if (properties == null) return null;
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return map;
    }

    public static Integer getInteger(Map<String, String> conf, String key, Integer defaultValue) {
        try {
            String v = conf.get(key);
            if (v == null) return defaultValue;
            return Integer.parseInt(v);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Double getDouble(Map<String, String> conf, String key, Double defaultValue) {
        try {
            String v = conf.get(key);
            if (v == null) return defaultValue;
            return Double.parseDouble(v);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Long getLong(Map<String, String> conf, String key, Long defaultValue) {
        try {
            String v = conf.get(key);
            if (v == null) return defaultValue;
            return Long.parseLong(v);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Boolean getBoolean(Map<String, String> conf, String key, Boolean defaultValue) {
        try {
            String v = conf.get(key);
            if (v == null) return defaultValue;
            return Boolean.parseBoolean(v);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String get(Map<String, String> conf, String key, String defaultValue) {
        try {
            String v = conf.get(key);
            if (v == null) return defaultValue;
            return v;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
