package com.netease.nim.camellia.core.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * abstract class
 * Created by caojiajun on 2023/3/6
 */
public abstract class CamelliaConfig {

    private Map<String, String> conf = new HashMap<>();
    private final List<ConfigUpdateCallback> callbackList = new ArrayList<>();

    protected void setConf(Map<String, String> conf) {
        this.conf = conf;
    }

    public Map<String, String> getConf() {
        return conf;
    }

    public void addCallback(ConfigUpdateCallback callback) {
        synchronized (callbackList) {
            callbackList.add(callback);
        }
    }

    protected List<ConfigUpdateCallback> getCallbackList() {
        return new ArrayList<>(callbackList);
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
