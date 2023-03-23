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

    @Autowired(required = false)
    private ConfigChangeNotify notify;

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
        notify(history);
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
        notify(history);
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
        notify(history);
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
        notify(history);
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
        notify(history);
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
        notify(history);
    }

    public ConfigHistoryPage getConfigHistoryListByType(int type, int offset, int limit, String keyword) {
        if (keyword != null) {
            keyword = keyword.trim();
        }
        List<ConfigHistory> list;
        long count;
        if (keyword == null || keyword.length() == 0) {
            list = dao.getConfigHistoryListByType(type, offset, limit);
            count = dao.getConfigHistoryListByTypeCount(type, offset, limit);
        } else {
            list = dao.getConfigHistoryListByTypeAndKeyword(type, offset, limit, keyword);
            count = dao.getConfigHistoryListByTypeAndKeywordCount(type, offset, limit, keyword);
        }
        return new ConfigHistoryPage(count, list);
    }

    public ConfigHistoryPage getConfigHistoryListByTypeAndConfigId(int type, long configId, int offset, int limit, String keyword) {
        if (keyword != null) {
            keyword = keyword.trim();
        }
        List<ConfigHistory> list;
        long count;
        if (keyword == null || keyword.length() == 0) {
            list = dao.getConfigHistoryListByTypeAndConfigId(type, configId, offset, limit);
            count = dao.getConfigHistoryListByTypeAndConfigIdCount(type, configId, offset, limit);
        } else {
            list = dao.getConfigHistoryListByTypeAndConfigIdAndKeyword(type, configId, offset, limit, keyword);
            count = dao.getConfigHistoryListByTypeAndConfigIdAndKeywordCount(type, configId, offset, limit, keyword);
        }
        return new ConfigHistoryPage(count, list);
    }

    public ConfigHistoryPage getConfigHistoryListByNamespace(String namespace, int offset, int limit, String keyword) {
        if (keyword != null) {
            keyword = keyword.trim();
        }
        List<ConfigHistory> list;
        long count;
        if (keyword == null || keyword.length() == 0) {
            list = dao.getConfigHistoryListByNamespace(namespace, offset, limit);
            count = dao.getConfigHistoryListByNamespaceCount(namespace, offset, limit);
        } else {
            list = dao.getConfigHistoryListByNamespaceAndKeyword(namespace, offset, limit, keyword);
            count = dao.getConfigHistoryListByNamespaceAndKeywordCount(namespace, offset, limit, keyword);
        }
        return new ConfigHistoryPage(count, list);
    }

    private void notify(ConfigHistory history) {
        if (notify != null) {
            notify.notify(history);
        }
    }
}

