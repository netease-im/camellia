package com.netease.nim.camellia.config.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.config.auth.EnvContext;
import com.netease.nim.camellia.config.conf.ConfigProperties;
import com.netease.nim.camellia.config.conf.LogBean;
import com.netease.nim.camellia.config.daowrapper.ConfigDaoWrapper;
import com.netease.nim.camellia.config.daowrapper.ConfigNamespaceDaoWrapper;
import com.netease.nim.camellia.config.exception.AppException;
import com.netease.nim.camellia.config.model.*;
import com.netease.nim.camellia.config.util.ConfigUtils;
import com.netease.nim.camellia.config.util.ParamCheckUtils;
import com.netease.nim.camellia.core.api.CamelliaApiCode;
import com.netease.nim.camellia.core.api.CamelliaConfigResponse;
import com.netease.nim.camellia.core.util.CacheUtil;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.toolkit.lock.CamelliaRedisLock;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;
import com.netease.nim.camellia.tools.utils.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by caojiajun on 2023/3/7
 */
@Service
public class ConfigService {

    private static final int maxNamespaceLen = 128;
    private static final int maxInfoLen = 4096;
    private static final int maxKeyLen = 256;
    private static final int maxOperatorInfoLen = 256;
    private static final int maxValueLen = 4096;

    @Autowired
    private ConfigDaoWrapper daoWrapper;

    @Autowired
    private ConfigProperties configProperties;

    @Autowired
    private ConfigHistoryService configHistoryService;

    @Autowired
    private CamelliaRedisTemplate template;

    @Autowired
    private ConfigNamespaceDaoWrapper configNamespaceDaoWrapper;

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
            List<Config> list = daoWrapper.findAllValidByNamespace(namespace);
            for (Config config : list) {
                map.put(config.getKey(), config.getValue());
            }
            localCache.put(TAG, namespace, map, configProperties.getLocalCacheExpireSeconds());
        }
        return map;
    }

    public ConfigPage getConfigList(String namespace, int offset, int limit, Integer validFlag, String keyword) {
        ParamCheckUtils.checkParam(namespace, "namespace", maxNamespaceLen);
        ParamCheckUtils.checkValidFlag(validFlag);
        return daoWrapper.getList(namespace, offset, limit, validFlag, keyword);
    }

    public String getConfigString(String namespace, int offset, int limit, Integer validFlag, String keyword) {
        ParamCheckUtils.checkParam(namespace, "namespace", maxNamespaceLen);
        ParamCheckUtils.checkValidFlag(validFlag);
        ConfigPage configPage = daoWrapper.getList(namespace, offset, limit, validFlag, keyword);
        StringBuilder builder = new StringBuilder();
        for (Config config : configPage.getList()) {
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
        ParamCheckUtils.checkParam(namespace, "namespace", maxNamespaceLen);
        ParamCheckUtils.checkParam(key, "key", maxKeyLen);
        return daoWrapper.findByNamespaceAndKey(namespace, key);
    }

    public int deleteConfig(String namespace, long id, String key, Long version, String operatorInfo) {
        ParamCheckUtils.checkParam(namespace, "namespace", maxNamespaceLen);
        ParamCheckUtils.checkParam(key, "key", maxKeyLen);
        ParamCheckUtils.checkParam(operatorInfo, "operatorInfo", maxOperatorInfoLen);
        ConfigNamespace configNamespace = configNamespaceDaoWrapper.getByNamespace(namespace);
        if (configNamespace == null || configNamespace.getValidFlag() == 0) {
            LogBean.get().addProps("config.namespace.not.valid", true);
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "config namespace not valid");
        }
        Config config = daoWrapper.getById(id);
        if (config == null) {
            LogBean.get().addProps("config.not.found", true);
            throw new AppException(HttpStatus.NOT_FOUND.value(), "config not found");
        }
        if (!config.getNamespace().equals(namespace)) {
            LogBean.get().addProps("namespace.not.equals", true);
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "id not belongs to this namespace");
        }
        if (!config.getKey().equals(key)) {
            LogBean.get().addProps("key.not.equals", true);
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "key not belongs to this id");
        }
        String lockKey = CacheUtil.buildCacheKey("config", namespace, config.getKey(), "~lock");
        CamelliaRedisLock lock = CamelliaRedisLock.newLock(template, lockKey, 5000, 5000);
        if (!lock.tryLock()) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "config concurrent update");
        }
        try {
            if (!Objects.equals(config.getVersion(), version)) {
                LogBean.get().addProps("config.has.changed", true);
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "config has changed");
            }
            int delete = daoWrapper.delete(config);
            LogBean.get().addProps("delete", delete);
            configHistoryService.configDelete(config, EnvContext.getUser(), operatorInfo);
            return delete;
        } finally {
            lock.release();
        }
    }

    public Config createOrUpdateConfig(String namespace, String key, String value, Integer type, String info, Long version, Integer validFlag, String operatorInfo) {
        ParamCheckUtils.checkParam(namespace, "namespace", maxNamespaceLen);
        ParamCheckUtils.checkParam(key, "key", maxKeyLen);
        ParamCheckUtils.checkParam(operatorInfo, "operatorInfo", maxOperatorInfoLen);
        ConfigNamespace configNamespace = configNamespaceDaoWrapper.getByNamespace(namespace);
        if (configNamespace == null || configNamespace.getValidFlag() == 0) {
            LogBean.get().addProps("config.namespace.not.valid", true);
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "config namespace not valid");
        }
        String lockKey = CacheUtil.buildCacheKey("config", namespace, key, "~lock");
        CamelliaRedisLock lock = CamelliaRedisLock.newLock(template, lockKey, 5000, 5000);
        if (!lock.tryLock()) {
            LogBean.get().addProps("config.concurrent.update", true);
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "config concurrent update");
        }
        try {
            ParamCheckUtils.checkValidFlag(validFlag);
            Config config = daoWrapper.findByNamespaceAndKey(namespace, key);
            if (config != null) {
                if (!Objects.equals(config.getVersion(), version)) {
                    LogBean.get().addProps("config.has.changed", true);
                    throw new AppException(HttpStatus.BAD_REQUEST.value(), "config has changed");
                }
            }
            long now = System.currentTimeMillis();
            if (config == null) {
                ParamCheckUtils.checkParam(value, "value", maxValueLen);
                ParamCheckUtils.checkParam(info, "info", maxInfoLen);
                if (type == null) {
                    LogBean.get().addProps("type.is.null", true);
                    throw new AppException(HttpStatus.BAD_REQUEST.value(), "type is null");
                }
                ConfigType configType = ConfigType.getByValue(type);
                if (configType == null) {
                    LogBean.get().addProps("type.is.illegal", true);
                    throw new AppException(HttpStatus.BAD_REQUEST.value(), "type is illegal");
                }
                config = new Config();
                config.setNamespace(namespace);
                config.setNamespaceId(configNamespace.getId());
                config.setCreateTime(now);
                config.setUpdateTime(now);
                config.setKey(key);
                config.setValue(value);
                config.setType(configType.getValue());
                config.setVersion(1L);
                config.setValidFlag(validFlag == null ? 0 : validFlag);
                config.setInfo(info);
                config.setCreator(EnvContext.getUser());
                config.setOperator(EnvContext.getUser());
                checkConfigType(config.getValue(), ConfigType.getByValue(config.getType()));
                int create = daoWrapper.create(config);
                LogBean.get().addProps("create", create);
                configHistoryService.configCreate(daoWrapper.findByNamespaceAndKey(namespace, key), operatorInfo);
            } else {
                Config oldConfig = ConfigUtils.duplicate(config);
                boolean needUpdate = false;
                if (validFlag != null) {
                    config.setValidFlag(validFlag);
                    needUpdate = true;
                }
                if (value != null) {
                    ParamCheckUtils.checkParam(value, "value", maxValueLen);
                    config.setValue(value);
                    needUpdate = true;
                }
                if (info != null) {
                    ParamCheckUtils.checkParam(info, "info", maxInfoLen);
                    config.setInfo(info);
                    needUpdate = true;
                }
                if (type != null) {
                    ConfigType configType = ConfigType.getByValue(type);
                    if (configType == null) {
                        LogBean.get().addProps("type.is.illegal", true);
                        throw new AppException(HttpStatus.BAD_REQUEST.value(), "type is illegal");
                    }
                    config.setType(configType.getValue());
                    needUpdate = true;
                }
                if (needUpdate) {
                    config.setUpdateTime(now);
                    checkConfigType(config.getValue(), ConfigType.getByValue(config.getType()));
                    config.setOperator(EnvContext.getUser());
                    config.setVersion(oldConfig.getVersion() + 1);
                    int update = daoWrapper.update(config);
                    LogBean.get().addProps("update", update);
                    Config newConfig = daoWrapper.findByNamespaceAndKey(namespace, key);
                    configHistoryService.configUpdate(oldConfig, newConfig, operatorInfo);
                }
            }
            return daoWrapper.findByNamespaceAndKey(namespace, key);
        } finally {
            lock.release();
        }
    }

    public ConfigHistoryPage getConfigNamespaceHistoryList(int offset, int limit, String keyword) {
        return configHistoryService.getConfigHistoryListByType(ConfigHistoryType.NAMESPACE.getValue(), offset, limit, keyword);
    }

    public ConfigHistoryPage getConfigHistoryListByNamespace(String namespace, int offset, int limit, String keyword) {
        ParamCheckUtils.checkParam(namespace, "namespace", maxNamespaceLen);
        return configHistoryService.getConfigHistoryListByNamespace(namespace, offset, limit, keyword);
    }

    public ConfigHistoryPage getConfigHistoryListByConfigKey(String namespace, String key, int offset, int limit, String keyword) {
        ParamCheckUtils.checkParam(namespace, "namespace", maxNamespaceLen);
        ParamCheckUtils.checkParam(key, "key", maxKeyLen);
        Config config = daoWrapper.getByKey(namespace, key);
        if (config == null) {
            LogBean.get().addProps("config.key.not.exists", true);
            throw new AppException(HttpStatus.NOT_FOUND.value(), "config key not found");
        }
        return configHistoryService.getConfigHistoryListByTypeAndConfigId(ConfigHistoryType.CONFIG.getValue(), config.getId(), offset, limit, keyword);
    }

    public ConfigHistoryPage getConfigHistoryListByConfigId(String namespace, Long configId, int offset, int limit, String keyword) {
        ParamCheckUtils.checkParam(namespace, "namespace", maxNamespaceLen);
        Config config = daoWrapper.getById(configId);
        if (config == null) {
            LogBean.get().addProps("config.id.not.exists", true);
            throw new AppException(HttpStatus.NOT_FOUND.value(), "config id not found");
        }
        if (!config.getNamespace().equals(namespace)) {
            if (!config.getNamespace().equals(namespace)) {
                LogBean.get().addProps("namespace.not.equals", true);
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "id not belongs to this namespace");
            }
        }
        ConfigHistoryPage configHistoryPage = configHistoryService.getConfigHistoryListByTypeAndConfigId(ConfigHistoryType.CONFIG.getValue(), configId, offset, limit, keyword);
        if (!configHistoryPage.getList().isEmpty()) {
            ConfigHistory history = configHistoryPage.getList().get(0);
            if (!history.getNamespace().equals(namespace)) {
                LogBean.get().addProps("namespace.not.equals", true);
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "id not belongs to this namespace");
            }
        }
        return configHistoryPage;
    }

    private void checkConfigType(String value, ConfigType type) {
        if (value == null) {
            LogBean.get().addProps("value.is.null", true);
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "value is null");
        }
        if (type == ConfigType.NUMBER) {
            try {
                Long.parseLong(value);
            } catch (Exception e) {
                LogBean.get().addProps("type.value.not.match", true);
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "type/value not match");
            }
        } else if (type == ConfigType.FLOAT_NUMBER) {
            try {
                Double.parseDouble(value);
            } catch (Exception e) {
                LogBean.get().addProps("type.value.not.match", true);
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "type/value not match");
            }
        } else if (type == ConfigType.JSON_STRING) {
            try {
                JSON.parseObject(value);
            } catch (Exception e) {
                LogBean.get().addProps("type.value.not.match", true);
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "type/value not match");
            }
        } else if (type == ConfigType.BOOLEAN) {
            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                LogBean.get().addProps("type.value.not.match", true);
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "type/value not match");
            }
        }
    }
}
