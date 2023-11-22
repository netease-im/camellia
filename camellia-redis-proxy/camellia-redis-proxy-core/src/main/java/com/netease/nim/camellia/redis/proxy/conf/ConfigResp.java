package com.netease.nim.camellia.redis.proxy.conf;

import com.alibaba.fastjson.JSON;
import com.netease.nim.camellia.tools.utils.MD5Util;

import java.util.*;

/**
 * Created by caojiajun on 2023/11/22
 */
public class ConfigResp {

    private final List<ConfigEntry> initConfig;
    private final String initConfigMd5;
    private final List<ConfigEntry> specialConfig;
    private final String specialConfigMd5;
    private final List<ConfigEntry> configs;
    private final String configsMd5;
    private final int size;

    public ConfigResp(Map<String, String> initConfigs, Map<String, String> configs) {
        this.initConfig = toList(initConfigs);
        this.initConfigMd5 = MD5Util.md5(JSON.toJSONString(initConfig));
        Map<String, String> specialConfigMap = new HashMap<>();
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String initValue = initConfigs.get(key);
            if (initValue == null || !initValue.equals(value)) {
                specialConfigMap.put(key, value);
            }
        }
        this.specialConfig = toList(specialConfigMap);
        this.specialConfigMd5 = MD5Util.md5(JSON.toJSONString(specialConfig));
        this.configs = toList(configs);
        this.configsMd5 = MD5Util.md5(JSON.toJSONString(this.configs));
        this.size = configs.size();
    }

    private static List<ConfigEntry> toList(Map<String, String> configMap) {
        List<ConfigEntry> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            list.add(new ConfigEntry(entry.getKey(), entry.getValue()));
        }
        Collections.sort(list);
        return list;
    }

    public List<ConfigEntry> getInitConfig() {
        return initConfig;
    }

    public String getInitConfigMd5() {
        return initConfigMd5;
    }

    public List<ConfigEntry> getSpecialConfig() {
        return specialConfig;
    }

    public String getSpecialConfigMd5() {
        return specialConfigMd5;
    }

    public List<ConfigEntry> getConfigs() {
        return configs;
    }

    public String getConfigsMd5() {
        return configsMd5;
    }

    public int getConfigSize() {
        return size;
    }

    public static class ConfigEntry implements Comparable<ConfigEntry> {
        private final String key;
        private final String value;
        public ConfigEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "ConfigEntry{key=" + key + ",value=" + value + "}";
        }

        @Override
        public int compareTo(ConfigEntry o) {
            String str1 = this.key + ":" + this.value;
            String str2 = o.key + ":" + o.value;
            return str1.compareTo(str2);
        }
    }
}
