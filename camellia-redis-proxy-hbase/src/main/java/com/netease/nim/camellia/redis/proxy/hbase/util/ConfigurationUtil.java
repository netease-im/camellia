package com.netease.nim.camellia.redis.proxy.hbase.util;

import java.util.Properties;

public class ConfigurationUtil {

    public static Integer getInteger(Properties properties, String key, Integer defaultValue) {
        try {
            Object v = properties.get(key);
            if (v == null) return defaultValue;
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Double getDouble(Properties properties, String key, Double defaultValue) {
        try {
            Object v = properties.get(key);
            if (v == null) return defaultValue;
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Long getLong(Properties properties, String key, Long defaultValue) {
        try {
            Object v = properties.get(key);
            if (v == null) return defaultValue;
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Boolean getBoolean(Properties properties, String key, Boolean defaultValue) {
        try {
            Object v = properties.get(key);
            if (v == null) return defaultValue;
            return Boolean.parseBoolean(String.valueOf(v));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String get(Properties properties, String key, String defaultValue) {
        try {
            Object v = properties.get(key);
            if (v == null) return defaultValue;
            return String.valueOf(v);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
