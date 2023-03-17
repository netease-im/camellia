package com.netease.nim.camellia.config.daowrapper;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.config.conf.ConfigProperties;
import com.netease.nim.camellia.config.dao.ConfigDao;
import com.netease.nim.camellia.config.model.Config;
import com.netease.nim.camellia.core.util.CacheUtil;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/3/7
 */
@Service
public class ConfigDaoWrapper {

    private static final String tag = "camellia_config";

    @Autowired
    private ConfigDao configDao;

    @Autowired
    private CamelliaRedisTemplate template;

    @Autowired
    private ConfigProperties configProperties;

    public List<Config> findAllValidByNamespace(String namespace) {
        String cacheKey = CacheUtil.buildCacheKey(tag, namespace);
        String value = template.get(cacheKey);
        if (value != null) {
            List<Config> list = new ArrayList<>();
            JSONArray array = JSONArray.parseArray(value);
            for (Object o : array) {
                Config config = JSONObject.parseObject(o.toString(), Config.class);
                list.add(config);
            }
            return list;
        }
        List<Config> configList = configDao.findAllValidByNamespace(namespace);
        template.setex(cacheKey, configProperties.getDaoCacheExpireSeconds(), JSONObject.toJSONString(configList));
        return configList;
    }

    public List<Config> getList(String namespace, int offset, int limit, boolean onlyValid, String keyword) {
        if (keyword != null) {
            keyword = keyword.trim();
        }
        if (keyword == null || keyword.length() == 0) {
            if (onlyValid) {
                return configDao.getValidList(namespace, offset, limit);
            } else {
                return configDao.getList(namespace, offset, limit);
            }
        } else {
            if (onlyValid) {
                return configDao.getValidListAndKeyword(namespace, offset, limit, keyword);
            } else {
                return configDao.getListAndKeyword(namespace, offset, limit, keyword);
            }
        }
    }

    public Config getById(long id) {
        return configDao.getById(id);
    }

    public int delete(Config config) {
        try {
            return configDao.deleteById(config.getId());
        } finally {
            clearCache(config.getNamespace());
        }
    }

    public Config findByNamespaceAndKey(String namespace, String key) {
        try {
            return configDao.findByNamespaceAndKey(namespace, key);
        } finally {
            clearCache(namespace);
        }
    }

    public int update(Config config) {
        try {
            return configDao.update(config);
        } finally {
            clearCache(config.getNamespace());
        }
    }

    public int create(Config config) {
        try {
            return configDao.create(config);
        } finally {
            clearCache(config.getNamespace());
        }
    }

    private void clearCache(String namespace) {
        String cacheKey = CacheUtil.buildCacheKey(tag, namespace);
        template.del(cacheKey);
    }

}
