package com.netease.nim.camellia.hot.key.sdk;

import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by caojiajun on 2023/5/7
 */
public class CamelliaHotKeyMonitorSdk extends CamelliaHotKeyAbstractSdk implements ICamelliaHotKeyMonitorSdk {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaHotKeyMonitorSdk.class);

    private final CamelliaHotKeySdk sdk;
    private final CamelliaHotKeyMonitorSdkConfig config;

    public CamelliaHotKeyMonitorSdk(CamelliaHotKeySdk sdk, CamelliaHotKeyMonitorSdkConfig config) {
        super(sdk, config.getExecutor(), config.getScheduler(), config.getHotKeyConfigReloadIntervalSeconds());
        this.sdk = sdk;
        this.config = config;
    }

    /**
     * 获取当配置
     * @return 配置
     */
    public CamelliaHotKeyMonitorSdkConfig getConfig() {
        return config;
    }

    @Override
    public void push(String namespace, String key) {
        if (rulePass(namespace, key) != null) {
            sdk.push(namespace, key, KeyAction.QUERY);
            if (logger.isDebugEnabled()) {
                logger.debug("push {} {}", namespace, key);
            }
        }
    }
}
