package com.netease.nim.camellia.hot.key.server.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigurationUtil {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationUtil.class);

    public static String getJsonString(String path) {
        byte[] buffer = null;
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {
            File file = new File(path);
            fis = new FileInputStream(file);
            bos = new ByteArrayOutputStream(1024);
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            buffer = bos.toByteArray();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        if (buffer == null) return null;
        return new String(buffer, StandardCharsets.UTF_8);
    }

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
