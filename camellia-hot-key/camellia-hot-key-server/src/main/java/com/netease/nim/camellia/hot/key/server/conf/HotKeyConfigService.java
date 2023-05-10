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
