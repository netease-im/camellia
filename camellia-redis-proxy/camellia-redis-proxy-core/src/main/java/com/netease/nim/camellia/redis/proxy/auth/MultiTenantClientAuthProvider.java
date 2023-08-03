package com.netease.nim.camellia.redis.proxy.auth;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.conf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by caojiajun on 2023/8/3
 */
public class MultiTenantClientAuthProvider implements ClientAuthProvider {

    private static final Logger logger = LoggerFactory.getLogger(MultiTenantClientAuthProvider.class);

    private List<MultiTenantConfig> multiTenantConfig;
    private MultiTenantConfigSelector selector;

    public MultiTenantClientAuthProvider() {
        this.multiTenantConfig = MultiTenantConfigUtils.getMultiTenantConfig();
        if (multiTenantConfig == null) {
            throw new IllegalArgumentException("multiTenantConfig init error");
        }
        this.selector = new MultiTenantConfigSelector(multiTenantConfig);
        logger.info("MultiTenantClientAuthProvider init, config = {}", JSONObject.toJSONString(multiTenantConfig));
        ProxyDynamicConf.registerCallback(() -> {
            List<MultiTenantConfig> config = MultiTenantConfigUtils.getMultiTenantConfig();
            if (config == null) {
                logger.error("MultiTenantConfig refresh failed, skip reload auth config");
                return;
            }
            update(config);
            logger.info("MultiTenantClientAuthProvider update, config = {}", JSONObject.toJSONString(config));
        });
    }

    private void update(List<MultiTenantConfig> multiTenantConfig) {
        this.multiTenantConfig = multiTenantConfig;
        this.selector = new MultiTenantConfigSelector(multiTenantConfig);
    }

    @Override
    public ClientIdentity auth(String userName, String password) {
        ClientIdentity clientIdentity = selector.selectClientIdentity(password);
        if (clientIdentity == null) {
            return ClientIdentity.AUTH_FAIL;
        }
        return clientIdentity;
    }

    @Override
    public boolean isPasswordRequired() {
        return true;
    }
}
