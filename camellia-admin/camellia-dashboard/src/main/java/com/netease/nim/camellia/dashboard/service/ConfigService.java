package com.netease.nim.camellia.dashboard.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.api.CamelliaApiCode;
import com.netease.nim.camellia.core.api.CamelliaConfigResponse;
import com.netease.nim.camellia.dashboard.conf.DashboardProperties;
import com.netease.nim.camellia.dashboard.daowrapper.ConfigDaoWrapper;
import com.netease.nim.camellia.dashboard.exception.AppException;
import com.netease.nim.camellia.dashboard.model.Config;
import com.netease.nim.camellia.dashboard.model.ConfigType;
import com.netease.nim.camellia.dashboard.util.LogBean;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;
import com.netease.nim.camellia.tools.utils.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by caojiajun on 2023/3/7
 */
@Service
public class ConfigService {

    @Autowired
    private ConfigDaoWrapper daoWrapper;

    @Autowired
    private DashboardProperties dashboardProperties;

    private static final String TAG = "config";
    private final CamelliaLocalCache localCache = new CamelliaLocalCache();

    public CamelliaConfigResponse getConfig(String namespace, String md5) {
        CamelliaConfigResponse response = new CamelliaConfigResponse();
        Map<String, String> confMap = confMap(namespace);
        String newMd5 = MD5Util.md5(JSONObject.toJSONString(confMap));
        if (newMd5.equals(md5)) {
            LogBean.get().addProps("not.modify", true);
            response.setCode(CamelliaApiCode.NOT_MODIFY.getCode());
            return response;
        }
        response.setCode(CamelliaApiCode.SUCCESS.getCode());
        response.setMd5(newMd5);
        response.setConf(confMap);
        return response;
    }

    private Map<String, String> confMap(String namespace) {
        Map<String, String> map = (Map<String, String>) localCache.get(TAG, namespace, Map.class);
        if (map == null) {
            map = new HashMap<>();
            List<Config> list = daoWrapper.findAll(namespace);
            for (Config config : list) {
                if (config.getValidFlag() == 1) {
                    map.put(config.getKey(), config.getValue());
                }
            }
            localCache.put(TAG, namespace, map, dashboardProperties.getLocalCacheExpireSeconds());
        }
        return map;
    }

    public List<Config> getConfigList(String namespace, boolean onlyValid) {
        List<Config> list = daoWrapper.findAll(namespace);
        if (onlyValid) {
            return list.stream().filter(config -> config.getValidFlag() == 1).collect(Collectors.toList());
        } else {
            return list;
        }
    }

    public String getConfigString(String namespace, boolean onlyValid) {
        List<Config> list = daoWrapper.findAll(namespace);
        if (onlyValid) {
            list = list.stream().filter(config -> config.getValidFlag() == 1).collect(Collectors.toList());
        }
        list.sort(Comparator.comparingLong(Config::getCreateTime));
        StringBuilder builder = new StringBuilder();
        for (Config config : list) {
            builder.append("## ").append(config.getInfo()).append("\r\n");
            builder.append("## id=").append(config.getId()).append("\r\n");
            if (config.getValidFlag() == 1) {
                builder.append(config.getKey()).append("=").append(config.getValue()).append("\r\n");
            } else {
                builder.append("#").append(config.getKey()).append("=").append(config.getValue()).append("\r\n");
            }
        }
        return builder.toString();
    }

    public Config getConfigByKey(String namespace, String key) {
        return daoWrapper.findByNamespaceAndKey(namespace, key);
    }

    public int deleteConfig(long id) {
        return daoWrapper.delete(id);
    }

    public Config createOrUpdateConfig(String namespace, String key, String value, Integer type, String info, Integer validFlag) {
        Config config = daoWrapper.findByNamespaceAndKey(namespace, key);
        long now = System.currentTimeMillis();
        if (config == null) {
            if (value == null) {
                throw new AppException(HttpStatus.BAD_GATEWAY.value(), "value is null");
            }
            if (type == null) {
                throw new AppException(HttpStatus.BAD_GATEWAY.value(), "type is null");
            }
            ConfigType configType = ConfigType.getByValue(type);
            if (configType == null) {
                throw new AppException(HttpStatus.BAD_GATEWAY.value(), "type is illegal");
            }
            config = new Config();
            config.setNamespace(namespace);
            config.setCreateTime(now);
            config.setUpdateTime(now);
            config.setKey(key);
            config.setValue(value);
            config.setType(configType.getValue());
            config.setValidFlag(validFlag == null ? 0 : validFlag);
            config.setInfo(info);
            checkConfigType(config.getValue(), ConfigType.getByValue(config.getType()));
            daoWrapper.save(config);
            LogBean.get().addProps("create", true);
        } else {
            boolean needUpdate = false;
            if (validFlag != null) {
                config.setValidFlag(validFlag);
                needUpdate = true;
            }
            if (value != null) {
                config.setValue(value);
                needUpdate = true;
            }
            if (info != null) {
                config.setInfo(info);
                needUpdate = true;
            }
            if (type != null) {
                ConfigType configType = ConfigType.getByValue(type);
                if (configType == null) {
                    throw new AppException(HttpStatus.BAD_GATEWAY.value(), "type is illegal");
                }
                config.setType(configType.getValue());
                needUpdate = true;
            }
            if (needUpdate) {
                config.setUpdateTime(now);
                checkConfigType(config.getValue(), ConfigType.getByValue(config.getType()));
                daoWrapper.save(config);
                LogBean.get().addProps("update", true);
            }
        }
        return daoWrapper.findByNamespaceAndKey(namespace, key);
    }

    private void checkConfigType(String value, ConfigType type) {
        if (value == null) {
            throw new AppException(HttpStatus.BAD_GATEWAY.value(), "value is null");
        }
        if (type == ConfigType.NUMBER) {
            try {
                Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new AppException(HttpStatus.BAD_GATEWAY.value(), "type/value not match");
            }
        } else if (type == ConfigType.FLOAT_NUMBER) {
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new AppException(HttpStatus.BAD_GATEWAY.value(), "type/value not match");
            }
        } else if (type == ConfigType.JSON_STRING) {
            try {
                JSON.parseObject(value);
            } catch (NumberFormatException e) {
                throw new AppException(HttpStatus.BAD_GATEWAY.value(), "type/value not match");
            }
        } else if (type == ConfigType.BOOLEAN) {
            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                throw new AppException(HttpStatus.BAD_GATEWAY.value(), "type/value not match");
            }
        }
    }
}
