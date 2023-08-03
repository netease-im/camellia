package com.netease.nim.camellia.redis.proxy.conf;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/8/3
 */
public class MultiTenantConfigUtils {

    private static final Logger logger = LoggerFactory.getLogger(MultiTenantConfigUtils.class);

    private static final String CONF_KEY = "multi.tenant.route.config";

    public static List<MultiTenantConfig> getMultiTenantConfig() {
        try {
            String string = ProxyDynamicConf.getString(CONF_KEY, null);
            if (string == null) {
                return new ArrayList<>();
            }
            List<MultiTenantConfig> list = new ArrayList<>();
            JSONArray array = JSONArray.parseArray(string);
            for (Object json : array) {
                MultiTenantConfig config = JSONObject.parseObject(json.toString(), MultiTenantConfig.class);
                if (!checkValid(config)) {
                    logger.warn("config = {} invalid", json);
                    return null;
                }
                list.add(config);
            }
            return list;
        } catch (Exception e) {
            logger.error("{} parse error", CONF_KEY, e);
            return null;
        }
    }

    public static boolean checkValid(MultiTenantConfig config) {
        try {
            if (config == null) return false;
            if (config.getName() == null) return false;
            if (config.getPassword() == null) return false;
            ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(config.getRoute());
            RedisResourceUtil.checkResourceTable(resourceTable);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
