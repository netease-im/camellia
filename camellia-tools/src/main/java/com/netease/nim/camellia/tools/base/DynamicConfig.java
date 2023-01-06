package com.netease.nim.camellia.tools.base;


/**
 * Created by caojiajun on 2023/1/4
 */
public interface DynamicConfig {

    String get(String key);

    static DynamicValueGetter<Long> wrapper(DynamicConfig config, String key, Long defaultValue) {
        return () -> {
            String value = config.get(key);
            if (value == null) {
                return defaultValue;
            }
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }

    static DynamicValueGetter<Integer> wrapper(DynamicConfig config, String key, Integer defaultValue) {
        return () -> {
            String value = config.get(key);
            if (value == null) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }

    static DynamicValueGetter<Double> wrapper(DynamicConfig config, String key, Double defaultValue) {
        return () -> {
            String value = config.get(key);
            if (value == null) {
                return defaultValue;
            }
            try {
                return Double.parseDouble(value);
            } catch (Exception e) {
                return defaultValue;
            }
        };
    }

    static DynamicValueGetter<String> wrapper(DynamicConfig config, String key, String defaultValue) {
        return () -> {
            String value = config.get(key);
            if (value == null) {
                return defaultValue;
            }
            return value;
        };
    }
}
