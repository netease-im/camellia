package com.netease.nim.camellia.redis.proxy.conf;

import com.alibaba.fastjson.JSONObject;

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

    public static <T> T get(Map<String, String> conf, String key, T defaultValue, Class<T> tClass) {
        try {
            String v = conf.get(key);
            if (v == null) return defaultValue;
            if (tClass == Integer.class) {
                return (T) Integer.valueOf(v);
            } else if (tClass == Boolean.class) {
                return (T) Boolean.valueOf(v);
            } else if (tClass == Long.class) {
                return (T) Long.valueOf(v);
            } else if (tClass == Double.class) {
                return (T) Double.valueOf(v);
            } else {
                return (T) v;
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Map<String, String> contentToMap(String content, ConfigContentType contentType) {
        if (contentType == ConfigContentType.properties) {
            String[] split = content.split("\n");
            Map<String, String> conf = new HashMap<>();
            for (String line : split) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                int index = line.indexOf("=");
                String key = line.substring(0, index);
                String value = line.substring(index + 1);
                conf.put(key, value);
            }
            return conf;
        } else if (contentType == ConfigContentType.json) {
            JSONObject json = JSONObject.parseObject(content);
            Map<String, String> conf = new HashMap<>();
            for (Map.Entry<String, Object> entry : json.entrySet()) {
                conf.put(entry.getKey(), entry.getValue().toString());
            }
            return conf;
        }
        return new HashMap<>();
    }

}
