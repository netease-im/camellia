package com.netease.nim.camellia.config.service;

import com.netease.nim.camellia.config.auth.EnvContext;
import com.netease.nim.camellia.config.conf.LogBean;
import com.netease.nim.camellia.config.daowrapper.ConfigNamespaceDaoWrapper;
import com.netease.nim.camellia.config.exception.AppException;
import com.netease.nim.camellia.config.model.ConfigNamespace;
import com.netease.nim.camellia.config.model.ConfigNamespacePage;
import com.netease.nim.camellia.config.util.ConfigUtils;
import com.netease.nim.camellia.config.util.ParamCheckUtils;
import com.netease.nim.camellia.core.util.CacheUtil;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.toolkit.lock.CamelliaRedisLock;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Created by caojiajun on 2023/3/15
 */
@Service
public class ConfigNamespaceService {

    private static final int maxNamespaceLen = 128;
    private static final int maxInfoLen = 4096;
    private static final int maxAliasLen = 32;

    @Autowired
    private ConfigNamespaceDaoWrapper dao;

    @Autowired
    private CamelliaRedisTemplate template;

    @Autowired
    private ConfigHistoryService configHistoryService;

    public ConfigNamespace createOrUpdateConfigNamespace(String namespace, String info, String alias, Integer version, Integer validFlag) {
        ParamCheckUtils.checkParam(namespace, "namespace", maxNamespaceLen);
        ParamCheckUtils.checkValidFlag(validFlag);
        String lockKey = CacheUtil.buildCacheKey("camellia_config_namespace", namespace, "~lock");
        CamelliaRedisLock lock = CamelliaRedisLock.newLock(template, lockKey, namespace, 5000, 5000);
        if (!lock.tryLock()) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "config namespace concurrent update");
        }
        try {
            ConfigNamespace configNamespace = dao.getByNamespace(namespace);
            if (configNamespace != null) {
                if (!Objects.equals(configNamespace.getVersion(), version)) {
                    LogBean.get().addProps("config.namespace.has.changed", true);
                    throw new AppException(HttpStatus.BAD_REQUEST.value(), "config namespace has changed");
                }
            }
            long now = System.currentTimeMillis();
            if (configNamespace == null) {
                ParamCheckUtils.checkParam(info, "info", maxInfoLen);
                ParamCheckUtils.checkParam(alias, "alias", maxAliasLen);
                configNamespace = new ConfigNamespace();
                configNamespace.setNamespace(namespace);
                configNamespace.setInfo(info);
                configNamespace.setAlias(alias);
                configNamespace.setValidFlag(validFlag == null ? 0 : 1);
                configNamespace.setCreateTime(now);
                configNamespace.setUpdateTime(now);
                configNamespace.setCreator(EnvContext.getUser());
                configNamespace.setOperator(EnvContext.getUser());
                configNamespace.setVersion(1);
                int create = dao.create(configNamespace);
                LogBean.get().addProps("create", create);
                configHistoryService.namespaceCreate(dao.getByNamespace(namespace));
            } else {
                ConfigNamespace oldConfig = ConfigUtils.duplicate(configNamespace);
                boolean needUpdate = false;
                if (info != null) {
                    ParamCheckUtils.checkParam(info, "info", maxInfoLen);
                    configNamespace.setInfo(info);
                    needUpdate = true;
                }
                if (alias != null) {
                    ParamCheckUtils.checkParam(alias, "alias", maxAliasLen);
                    configNamespace.setAlias(alias);
                    needUpdate = true;
                }
                if (validFlag != null) {
                    ParamCheckUtils.checkValidFlag(validFlag);
                    configNamespace.setValidFlag(validFlag);
                    needUpdate = true;
                }
                if (needUpdate) {
                    configNamespace.setUpdateTime(now);
                    configNamespace.setOperator(EnvContext.getUser());
                    configNamespace.setVersion(oldConfig.getVersion() + 1);
                    int update = dao.update(configNamespace);
                    LogBean.get().addProps("update", update);
                    ConfigNamespace newConfig = dao.getByNamespace(namespace);
                    configHistoryService.namespaceUpdate(oldConfig, newConfig);
                }
            }
            return dao.getByNamespace(namespace);
        } finally {
            lock.release();
        }
    }

    public int deleteConfigNamespace(long id, String namespace, Integer version) {
        ParamCheckUtils.checkParam(namespace, "namespace", maxNamespaceLen);
        String lockKey = CacheUtil.buildCacheKey("camellia_config_namespace", namespace, "~lock");
        CamelliaRedisLock lock = CamelliaRedisLock.newLock(template, lockKey, namespace, 5000, 5000);
        if (!lock.tryLock()) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "config namespace concurrent update");
        }
        try {
            ConfigNamespace configNamespace = dao.getById(id);
            if (configNamespace == null) {
                LogBean.get().addProps("config.namespace.not.found", true);
                throw new AppException(HttpStatus.NOT_FOUND.value(), "config namespace not found");
            }
            if (!configNamespace.getNamespace().equals(namespace)) {
                LogBean.get().addProps("id.namespace.not.match", true);
                throw new AppException(HttpResponseStatus.BAD_REQUEST.code(), "id/namespace not match");
            }
            if (!Objects.equals(configNamespace.getVersion(), version)) {
                LogBean.get().addProps("config.namespace.has.changed", true);
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "config namespace has changed");
            }
            int delete = dao.delete(configNamespace);
            LogBean.get().addProps("delete", delete);
            configHistoryService.namespaceDelete(configNamespace, EnvContext.getUser());
            return delete;
        } finally {
            lock.release();
        }
    }

    public ConfigNamespacePage getList(int offset, int limit, Integer validFlag, String keyword) {
        ParamCheckUtils.checkValidFlag(validFlag);
        return dao.getList(offset, limit, validFlag, keyword);
    }

}
