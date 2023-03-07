package com.netease.nim.camellia.dashboard.daowrapper;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.util.CacheUtil;
import com.netease.nim.camellia.dashboard.conf.DashboardProperties;
import com.netease.nim.camellia.dashboard.dao.ConfigDao;
import com.netease.nim.camellia.dashboard.model.Config;
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
    private DashboardProperties dashboardProperties;

    public List<Config> findAll(String namespace) {
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
        List<Config> configList = configDao.findByNamespace(namespace);
        template.setex(cacheKey, dashboardProperties.getDaoCacheExpireSeconds(), JSONObject.toJSONString(configList));
        return configList;
    }

    public int delete(long id) {
        Config config = configDao.getOne(id);
        try {
            configDao.deleteById(id);
            return 1;
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

    public int save(Config config) {
        try {
            configDao.save(config);
            return 1;
        } finally {
            clearCache(config.getNamespace());
        }
    }

    private void clearCache(String namespace) {
        String cacheKey = CacheUtil.buildCacheKey(tag, namespace);
        template.del(cacheKey);
    }

}
