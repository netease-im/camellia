package com.netease.nim.camellia.redis.proxy.route;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.conf.MultiTenantConfig;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 对json格式的配置友好
 * Created by caojiajun on 2026/2/11
 */
public class MultiTenantsV2RouteConfProvider extends MultiTenantsRouteConfProvider {

    private static final Logger logger = LoggerFactory.getLogger(MultiTenantsV2RouteConfProvider.class);

    private static final String CONF_KEY = "multi.tenant.route.config";

    public List<MultiTenantConfig> getMultiTenantConfig() {
        try {
            String string = ProxyDynamicConf.getString(CONF_KEY, null);
            if (string == null) {
                return new ArrayList<>();
            }
            List<MultiTenantConfig> list = new ArrayList<>();
            JSONArray array = JSONArray.parseArray(string);
            for (Object json : array) {
                MultiTenantConfig config = JSONObject.parseObject(json.toString(), MultiTenantConfig.class);
                checkValid(config);
                list.add(config);
            }
            return list;
        } catch (Exception e) {
            logger.error("{} parse error", CONF_KEY, e);
            return null;
        }
    }

}
