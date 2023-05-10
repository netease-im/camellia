package com.netease.nim.camellia.hot.key.server.callback;

import com.netease.nim.camellia.hot.key.common.model.HotKey;
import com.netease.nim.camellia.hot.key.server.utils.TimeCache;
import com.netease.nim.camellia.hot.key.server.bean.BeanInitUtils;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.tools.cache.NamespaceCamelliaLocalCache;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/5/10
 */
public class HotKeyCallbackManager {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyCallbackManager.class);

    private final HotKeyServerProperties properties;
    private final ThreadPoolExecutor executor;
    private final HotKeyCallback hotKeyCallback;
    private final NamespaceCamelliaLocalCache callbackTimeCache;

    public HotKeyCallbackManager(HotKeyServerProperties properties) {
        this.properties = properties;
        this.hotKeyCallback = (HotKeyCallback) properties.getBeanFactory().getBean(BeanInitUtils.parseClass(properties.getHotKeyCallbackClassName()));
        this.callbackTimeCache = new NamespaceCamelliaLocalCache(properties.getMaxNamespace(), properties.getCacheCapacityPerNamespace());
        this.executor = new ThreadPoolExecutor(properties.getCallbackExecutorSize(), properties.getCallbackExecutorSize(), 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10000), new CamelliaThreadFactory("hot-key-callback"), new ThreadPoolExecutor.DiscardPolicy());
    }

    public void newHotkey(HotKey hotKey) {
        try {
            if (hotKeyCallback != null) {
                Long lastCallbackTime = callbackTimeCache.get(hotKey.getNamespace(), hotKey.getKey(), Long.class);
                if (lastCallbackTime == null) {
                    boolean success = callbackTimeCache.putIfAbsent(hotKey.getNamespace(), hotKey.getKey(),
                            TimeCache.currentMillis, properties.getHotKeyCallbackIntervalSeconds());
                    if (success) {
                        executor.submit(() -> {
                            try {
                                hotKeyCallback.newHotKey(hotKey);
                            } catch (Exception e) {
                                logger.error("hotKey callback error", e);
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            logger.error("hotKey callback error", e);
        }
    }

    public void topN() {

    }
}