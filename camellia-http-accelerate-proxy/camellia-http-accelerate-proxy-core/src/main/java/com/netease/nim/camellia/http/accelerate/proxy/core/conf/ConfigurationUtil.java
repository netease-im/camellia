package com.netease.nim.camellia.http.accelerate.proxy.core.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 *
 * Created by hzcaojiajun on 2018/7/18.
 */
public class ConfigurationUtil {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationUtil.class);

    public static Properties load(String path) {
        try {
            InputStream inputStream = ClassLoader.getSystemResourceAsStream(path);
            if (inputStream == null) {
                return null;
            }
            Properties prop = new Properties();
            prop.load(inputStream);
            return prop;
        } catch (Exception e) {
            logger.error("load error", e);
            return null;
        }
    }

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

    public static Properties loadByFilePath(String path) {
        FileInputStream fis = null;
        try {
            File file = new File(path);
            if (!file.exists()) {
                logger.warn("{} not exists", path);
                return null;
            }
            fis = new FileInputStream(path);
            Properties prop = new Properties();
            prop.load(fis);
            return prop;
        } catch (Exception e) {
            logger.error("loadByFilePath error", e);
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.error("close error", e);
                }
            }
        }
    }

    public static Integer getInteger(Map<String, String> properties, String key, Integer defaultValue) {
        try {
            Object v = properties.get(key);
            if (v == null) return defaultValue;
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Double getDouble(Map<String, String> properties, String key, Double defaultValue) {
        try {
            Object v = properties.get(key);
            if (v == null) return defaultValue;
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Long getLong(Map<String, String> properties, String key, Long defaultValue) {
        try {
            Object v = properties.get(key);
            if (v == null) return defaultValue;
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Boolean getBoolean(Map<String, String> properties, String key, Boolean defaultValue) {
        try {
            Object v = properties.get(key);
            if (v == null) return defaultValue;
            return Boolean.parseBoolean(String.valueOf(v));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String get(Map<String, String> properties, String key, String defaultValue) {
        try {
            Object v = properties.get(key);
            if (v == null) return defaultValue;
            return String.valueOf(v);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
