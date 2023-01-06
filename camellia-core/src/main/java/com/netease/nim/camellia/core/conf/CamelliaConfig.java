package com.netease.nim.camellia.core.conf;

import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2022/11/16
 */
public class CamelliaConfig {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaConfig.class);

    private static Map<String, String> conf = new HashMap<>();
    private static final AtomicBoolean init = new AtomicBoolean();

    private final String fileName;

    public CamelliaConfig() {
        this("camellia.properties");
    }

    public CamelliaConfig(String fileName) {
        this.fileName = fileName;
        init();
    }

    private void init() {
        if (init.compareAndSet(false, true)) {
            reload();
            Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-config"))
                    .scheduleAtFixedRate(this::reload, 60, 60, TimeUnit.SECONDS);
        }
    }

    private void reload() {
        URL url = CamelliaConfig.class.getClassLoader().getResource(fileName);
        if (url == null) return;
        try {
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(url.getPath()));
            } catch (IOException e) {
                props.load(CamelliaConfig.class.getClassLoader().getResourceAsStream(fileName));
            }
            Map<String, String> map = new HashMap<>();
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            if (map.equals(CamelliaConfig.conf)) {
                logger.debug("CamelliaFeignConfig skip reload for conf not modify");
            } else {
                CamelliaConfig.conf = map;
                logger.info("CamelliaFeignConfig reload success.");
            }
        } catch (Exception e) {
            logger.error("reload {} error", fileName);
        }
    }

    public Map<String, String> getConf() {
        return conf;
    }

    public long getLong(long bid, String bgroup, String key, long defaultValue) {
        Long value = getLong(bid + "." + bgroup + "." + key, null);
        if (value == null) {
            value = getLong(bid + "." + key, null);
        }
        if (value == null) {
            value = getLong("default." + key, null);
        }
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public boolean getBoolean(long bid, String bgroup, String key, boolean defaultValue) {
        Boolean value = getBoolean(bid + "." + bgroup + "." + key, null);
        if (value == null) {
            value = getBoolean(bid + "." + key, null);
        }
        if (value == null) {
            value = getBoolean("default." + key, null);
        }
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public int getInteger(long bid, String bgroup, String key, int defaultValue) {
        Integer value = getInteger(bid + "." + bgroup + "." + key, null);
        if (value == null) {
            value = getInteger(bid + "." + key, null);
        }
        if (value == null) {
            value = getInteger("default." + key, null);
        }
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public double getDouble(long bid, String bgroup, String key, double defaultValue) {
        Double value = getDouble(bid + "." + bgroup + "." + key, null);
        if (value == null) {
            value = getDouble(bid + "." + key, null);
        }
        if (value == null) {
            value = getDouble("default." + key, null);
        }
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public Long getLong(String key, Long defaultValue) {
        try {
            String value = conf.get(key);
            if (value == null) return defaultValue;
            return Long.parseLong(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Integer getInteger(String key, Integer defaultValue) {
        try {
            String value = conf.get(key);
            if (value == null) return defaultValue;
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Double getDouble(String key, Double defaultValue) {
        try {
            String value = conf.get(key);
            if (value == null) return defaultValue;
            return Double.parseDouble(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        try {
            String value = conf.get(key);
            if (value == null) return defaultValue;
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }


    public String getString(String key, String defaultValue) {
        try {
            String value = conf.get(key);
            if (value == null) return defaultValue;
            return value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

}
