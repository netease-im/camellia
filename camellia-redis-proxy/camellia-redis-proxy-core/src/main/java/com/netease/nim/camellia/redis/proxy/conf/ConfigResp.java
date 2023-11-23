package com.netease.nim.camellia.redis.proxy.conf;

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
    private final List<ConfigEntry> allConfig;
    private final String allConfigMd5;

    public ConfigResp(Map<String, String> initConfigs, Map<String, String> configs) {
        this.initConfig = toList(initConfigs);
        this.initConfigMd5 = MD5Util.md5(this.initConfig.toString());
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
        this.specialConfigMd5 = MD5Util.md5(this.specialConfig.toString());
        this.allConfig = toList(configs);
        this.allConfigMd5 = MD5Util.md5(this.allConfig.toString());
    }

    public static List<ConfigEntry> toList(Map<String, String> configMap) {
        List<ConfigEntry> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            list.add(new ConfigEntry(entry.getKey(), entry.getValue()));
        }
        Collections.sort(list);
        return list;
    }

    public static Map<String, String> toMap(List<ConfigEntry> list) {
        Map<String, String> map = new HashMap<>();
        for (ConfigEntry entry : list) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
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

    public List<ConfigEntry> getAllConfig() {
        return allConfig;
    }

    public String getAllConfigMd5() {
        return allConfigMd5;
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
