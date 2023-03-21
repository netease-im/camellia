package com.netease.nim.camellia.config.daowrapper;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.config.conf.ConfigProperties;
import com.netease.nim.camellia.config.dao.ConfigNamespaceDao;
import com.netease.nim.camellia.config.model.ConfigNamespace;
import com.netease.nim.camellia.config.model.ConfigNamespacePage;
import com.netease.nim.camellia.core.util.CacheUtil;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by caojiajun on 2023/3/15
 */
@Service
public class ConfigNamespaceDaoWrapper {

    private static final String tag = "camellia_config_namespace";

    @Autowired
    private ConfigNamespaceDao dao;

    @Autowired
    private CamelliaRedisTemplate template;

    @Autowired
    private ConfigProperties configProperties;

    public int create(ConfigNamespace configNamespace) {
        try {
            return dao.create(configNamespace);
        } finally {
            clearCache(configNamespace.getNamespace());
        }
    }

    public int update(ConfigNamespace configNamespace) {
        try {
            return dao.update(configNamespace);
        } finally {
            clearCache(configNamespace.getNamespace());
        }
    }

    public int delete(ConfigNamespace configNamespace) {
        try {
            return dao.delete(configNamespace.getId());
        } finally {
            clearCache(configNamespace.getNamespace());
        }
    }

    public ConfigNamespace getById(long id) {
        return dao.getById(id);
    }

    public ConfigNamespacePage getList(int offset, int limit, boolean onlyValid, String keyword) {
        if (keyword != null) {
            keyword = keyword.trim();
        }
        if (keyword == null || keyword.length() == 0) {
            List<ConfigNamespace> list;
            long count;
            if (onlyValid) {
                list = dao.getValidList(offset, limit);
                count = dao.getValidListCount(offset, limit);
            } else {
                list = dao.getList(offset, limit);
                count = dao.getListCount(offset, limit);
            }
            return new ConfigNamespacePage(count, list);
        } else {
            List<ConfigNamespace> list;
            long count;
            if (onlyValid) {
                list = dao.getValidListAndKeyword(offset, limit, keyword);
                count = dao.getValidListAndKeywordCount(offset, limit, keyword);
            } else {
                list = dao.getListAndKeyword(offset, limit, keyword);
                count = dao.getListAndKeywordCount(offset, limit, keyword);
            }
            return new ConfigNamespacePage(count, list);
        }
    }

    public ConfigNamespace getByNamespace(String namespace) {
        String cacheKey = CacheUtil.buildCacheKey(tag, namespace);
        String value = template.get(cacheKey);
        if (value != null) {
            if (value.equalsIgnoreCase("null")) {
                return null;
            }
            return JSONObject.parseObject(value, ConfigNamespace.class);
        }
        ConfigNamespace configNamespace = dao.getByNamespace(namespace);
        if (configNamespace != null) {
            template.setex(cacheKey, configProperties.getDaoCacheExpireSeconds(), JSONObject.toJSONString(configNamespace));
        } else {
            template.setex(cacheKey, configProperties.getDaoCacheExpireSeconds(), "null");
        }
        return configNamespace;
    }

    private void clearCache(String namespace) {
        String cacheKey = CacheUtil.buildCacheKey(tag, namespace);
        template.del(cacheKey);
    }
}
