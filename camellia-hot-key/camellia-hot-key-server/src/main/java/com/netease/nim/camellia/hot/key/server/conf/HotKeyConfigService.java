package com.netease.nim.camellia.hot.key.server.conf;

import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/5/9
 */
public abstract class HotKeyConfigService {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyConfigService.class);

    private final List<Callback> callbackList = new ArrayList<>();

    /**
     * 获取HotKeyConfig
     * @param namespace namespace
     * @return HotKeyConfig
     */
    public abstract HotKeyConfig get(String namespace);

    /**
     * 初始化后会调用本方法，你可以重写本方法去获取到HotKeyServerProperties中的相关配置
     * @param properties properties
     */
    public void init(HotKeyServerProperties properties) {

    }

    protected final void invokeUpdate(String namespace) {
        for (Callback callback : callbackList) {
            try {
                callback.update(namespace);
            } catch (Exception e) {
                logger.error("callback error, namespace = {}", namespace, e);
            }
        }
    }

    public final synchronized void registerCallback(Callback callback) {
        callbackList.add(callback);
    }

    public static interface Callback {
        void update(String namespace);
    }
}
