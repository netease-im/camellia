package com.netease.nim.camellia.config.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.netease.nim.camellia.config.conf.LogBean;
import com.netease.nim.camellia.config.dao.ConfigHistoryDao;
import com.netease.nim.camellia.config.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Created by caojiajun on 2023/3/15
 */
@Service
public class ConfigHistoryService {

    @Autowired
    private ConfigHistoryDao dao;

    public void namespaceUpdate(ConfigNamespace oldConfig, ConfigNamespace newConfig) {
        ConfigHistory history = new ConfigHistory();
        history.setType(ConfigHistoryType.NAMESPACE.getValue());
        history.setOperator(newConfig.getOperator());
        history.setNamespace(newConfig.getNamespace());
        history.setConfigId(newConfig.getId());
        history.setOldConfig(JSONObject.toJSONString(oldConfig, SerializerFeature.SortField));
        history.setNewConfig(JSONObject.toJSONString(newConfig, SerializerFeature.SortField));
        if (Objects.equals(oldConfig.getValidFlag(), newConfig.getValidFlag())) {
            history.setOperatorType(OperatorType.UPDATE.name());
        } else {
            if (newConfig.getValidFlag() == 1) {
                history.setOperatorType(OperatorType.VALID.name());
            } else if (newConfig.getValidFlag() == 0) {
                history.setOperatorType(OperatorType.INVALID.name());
            } else {
                history.setOperatorType(OperatorType.UPDATE.name());
            }
        }
        history.setCreateTime(System.currentTimeMillis());
        int create = dao.create(history);
        LogBean.get().addProps("config.history.insert", create);
    }

    public void namespaceCreate(ConfigNamespace configNamespace) {
        ConfigHistory history = new ConfigHistory();
        history.setType(ConfigHistoryType.NAMESPACE.getValue());
        history.setOperator(configNamespace.getCreator());
        history.setConfigId(configNamespace.getId());
        history.setNamespace(configNamespace.getNamespace());
        history.setNewConfig(JSONObject.toJSONString(configNamespace, SerializerFeature.SortField));
        history.setOldConfig("");
        history.setOperatorType(OperatorType.CREATE.name());
        history.setCreateTime(System.currentTimeMillis());
        int create = dao.create(history);
        LogBean.get().addProps("config.history.insert", create);
    }

    public void namespaceDelete(ConfigNamespace configNamespace, String user) {
        ConfigHistory history = new ConfigHistory();
        history.setType(ConfigHistoryType.NAMESPACE.getValue());
        history.setConfigId(configNamespace.getId());
        history.setNamespace(configNamespace.getNamespace());
        history.setOldConfig(JSONObject.toJSONString(configNamespace, SerializerFeature.SortField));
        history.setNewConfig("");
        history.setOperatorType(OperatorType.DELETE.name());
        history.setCreateTime(System.currentTimeMillis());
        history.setOperator(user);
        int create = dao.create(history);
        LogBean.get().addProps("config.history.insert", create);
    }

    public void configUpdate(Config oldConfig, Config newConfig) {
        ConfigHistory history = new ConfigHistory();
        history.setType(ConfigHistoryType.CONFIG.getValue());
        history.setOperator(newConfig.getOperator());
        history.setNamespace(newConfig.getNamespace());
        history.setConfigId(newConfig.getId());
        history.setOldConfig(JSONObject.toJSONString(oldConfig, SerializerFeature.SortField));
        history.setNewConfig(JSONObject.toJSONString(newConfig, SerializerFeature.SortField));
        if (Objects.equals(oldConfig.getValidFlag(), newConfig.getValidFlag())) {
            history.setOperatorType(OperatorType.UPDATE.name());
        } else {
            if (newConfig.getValidFlag() == 1) {
                history.setOperatorType(OperatorType.VALID.name());
            } else if (newConfig.getValidFlag() == 0) {
                history.setOperatorType(OperatorType.INVALID.name());
            } else {
                history.setOperatorType(OperatorType.UPDATE.name());
            }
        }
        history.setCreateTime(System.currentTimeMillis());
        int create = dao.create(history);
        LogBean.get().addProps("config.history.insert", create);
    }

    public void configCreate(Config config) {
        ConfigHistory history = new ConfigHistory();
        history.setType(ConfigHistoryType.CONFIG.getValue());
        history.setOperator(config.getOperator());
        history.setConfigId(config.getId());
        history.setNamespace(config.getNamespace());
        history.setNewConfig(JSONObject.toJSONString(config, SerializerFeature.SortField));
        history.setOldConfig("");
        history.setOperatorType(OperatorType.CREATE.name());
        history.setCreateTime(System.currentTimeMillis());
        int create = dao.create(history);
        LogBean.get().addProps("config.history.insert", create);
    }

    public void configDelete(Config config, String user) {
        ConfigHistory history = new ConfigHistory();
        history.setType(ConfigHistoryType.CONFIG.getValue());
        history.setConfigId(config.getId());
        history.setNamespace(config.getNamespace());
        history.setOldConfig(JSONObject.toJSONString(config, SerializerFeature.SortField));
        history.setNewConfig("");
        history.setOperatorType(OperatorType.DELETE.name());
        history.setCreateTime(System.currentTimeMillis());
        history.setOperator(user);
        int create = dao.create(history);
        LogBean.get().addProps("config.history.insert", create);
    }

    public List<ConfigHistory> getConfigHistoryListByType(int type, int offset, int limit, String keyword) {
        if (keyword != null) {
            keyword = keyword.trim();
        }
        if (keyword == null || keyword.length() == 0) {
            return dao.getConfigHistoryListByType(type, offset, limit);
        } else {
            return dao.getConfigHistoryListByTypeAndKeyword(type, offset, limit, keyword);
        }
    }

    public List<ConfigHistory> getConfigHistoryListByTypeAndConfigId(int type, long configId, int offset, int limit, String keyword) {
        if (keyword != null) {
            keyword = keyword.trim();
        }
        if (keyword == null || keyword.length() == 0) {
            return dao.getConfigHistoryListByTypeAndConfigId(type, configId, offset, limit);
        } else {
            return dao.getConfigHistoryListByTypeAndConfigIdAndKeyword(type, configId, offset, limit, keyword);
        }
    }

    public List<ConfigHistory> getConfigHistoryListByNamespace(String namespace, int offset, int limit, String keyword) {
        if (keyword != null) {
            keyword = keyword.trim();
        }
        if (keyword == null || keyword.length() == 0) {
            return dao.getConfigHistoryListByNamespace(namespace, offset, limit);
        } else {
            return dao.getConfigHistoryListByNamespaceAndKeyword(namespace, offset, limit, keyword);
        }
    }
}
