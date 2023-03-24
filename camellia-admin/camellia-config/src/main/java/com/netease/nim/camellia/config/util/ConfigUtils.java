package com.netease.nim.camellia.config.util;

import com.netease.nim.camellia.config.model.Config;
import com.netease.nim.camellia.config.model.ConfigNamespace;

/**
 * Created by caojiajun on 2023/3/15
 */
public class ConfigUtils {

    public static ConfigNamespace duplicate(ConfigNamespace configNamespace) {
        ConfigNamespace duplicate = new ConfigNamespace();
        duplicate.setId(configNamespace.getId());
        duplicate.setNamespace(configNamespace.getNamespace());
        duplicate.setAlias(configNamespace.getAlias());
        duplicate.setInfo(configNamespace.getInfo());
        duplicate.setCreator(configNamespace.getCreator());
        duplicate.setOperator(configNamespace.getOperator());
        duplicate.setVersion(configNamespace.getVersion());
        duplicate.setValidFlag(configNamespace.getValidFlag());
        duplicate.setCreateTime(configNamespace.getCreateTime());
        duplicate.setUpdateTime(configNamespace.getUpdateTime());
        return duplicate;
    }

    public static Config duplicate(Config config) {
        Config duplicate = new Config();
        duplicate.setId(config.getId());
        duplicate.setNamespaceId(config.getNamespaceId());
        duplicate.setNamespace(config.getNamespace());
        duplicate.setKey(config.getKey());
        duplicate.setValue(config.getValue());
        duplicate.setType(config.getType());
        duplicate.setInfo(config.getInfo());
        duplicate.setVersion(config.getVersion());
        duplicate.setValidFlag(config.getValidFlag());
        duplicate.setCreator(config.getCreator());
        duplicate.setOperator(config.getOperator());
        duplicate.setCreateTime(config.getCreateTime());
        duplicate.setUpdateTime(config.getUpdateTime());
        return duplicate;
    }
}
