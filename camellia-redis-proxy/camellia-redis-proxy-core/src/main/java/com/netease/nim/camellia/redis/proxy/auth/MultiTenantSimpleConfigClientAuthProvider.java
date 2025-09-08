package com.netease.nim.camellia.redis.proxy.auth;

import com.netease.nim.camellia.redis.proxy.conf.MultiTenantConfigUtils;
import com.netease.nim.camellia.redis.proxy.conf.MultiTenantSimpleConfig;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2025/9/8
 */
public class MultiTenantSimpleConfigClientAuthProvider implements ClientAuthProvider {

    private static final Logger logger = LoggerFactory.getLogger(MultiTenantSimpleConfigClientAuthProvider.class);
    private Map<String, ClientIdentity> map = new ConcurrentHashMap<>();

    public MultiTenantSimpleConfigClientAuthProvider() {
        update();
        ProxyDynamicConf.registerCallback(this::update);
    }

    @Override
    public ClientIdentity auth(String userName, String password) {
        ClientIdentity clientIdentity = map.get(password);
        if (clientIdentity == null) {
            return ClientIdentity.AUTH_FAIL;
        }
        return clientIdentity;
    }

    @Override
    public boolean isPasswordRequired() {
        return true;
    }

    private void update() {
        try {
            List<MultiTenantSimpleConfig> configs = MultiTenantConfigUtils.getMultiTenantSimpleConfig();
            Map<String, ClientIdentity> map = new ConcurrentHashMap<>();
            for (MultiTenantSimpleConfig config : configs) {
                ClientIdentity clientIdentity = new ClientIdentity();
                clientIdentity.setBgroup(config.getName());
                clientIdentity.setBid(1L);
                clientIdentity.setPass(true);
                map.put(config.getPassword(), clientIdentity);
            }
            this.map = map;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
