package com.netease.nim.camellia.redis.proxy.nacos.springboot;

import com.alibaba.nacos.api.config.ConfigService;
import com.netease.nim.camellia.redis.proxy.springboot.ProxyDynamicConfSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by caojiajun on 2021/10/18
 */
public class NacosProxyDynamicConfSupport implements ProxyDynamicConfSupport {

    private static final Logger logger = LoggerFactory.getLogger(NacosProxyDynamicConfSupport.class);

    private ConfigService configService;
    private final List<ReloadCallback> reloadCallbackList = new ArrayList<>();

    public ConfigService getConfigService() {
        return configService;
    }

    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    public void addReloadCallback(ReloadCallback reloadCallback) {
        reloadCallbackList.add(reloadCallback);
    }

    public List<ReloadCallback> getReloadCallbackList() {
        return Collections.unmodifiableList(reloadCallbackList);
    }

    @Override
    public void reload() {
        for (ReloadCallback reloadCallback : reloadCallbackList) {
            try {
                reloadCallback.reload();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public interface ReloadCallback {
        void reload() throws Exception;
    }
}
