package com.netease.nim.camellia.hot.key.sdk.netty;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.hot.key.common.model.HotKey;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyPackBizHandler;
import com.netease.nim.camellia.hot.key.common.netty.pack.NotifyHotKeyConfigPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.NotifyHotKeyConfigRepPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.NotifyHotKeyPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.NotifyHotKeyRepPack;
import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyPackBizClientHandler implements HotKeyPackBizHandler {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyPackBizClientHandler.class);

    private final List<HotKeyClientListener> listeners = new ArrayList<>();
    private final ThreadPoolExecutor executor;

    public HotKeyPackBizClientHandler(int workThread, int queueCapacity) {
        this.executor = new ThreadPoolExecutor(workThread, workThread, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity), new DefaultThreadFactory("camellia-hot-key-pack-consumer"), new ThreadPoolExecutor.AbortPolicy());
    }

    public synchronized void registerListener(HotKeyClientListener listener) {
        listeners.add(listener);
        logger.info("HotKeyClientListener register success, listener = {}", listener);
    }

    @Override
    public CompletableFuture<NotifyHotKeyRepPack> onNotifyHotKeyPack(Channel channel, NotifyHotKeyPack pack) {
        CompletableFuture<NotifyHotKeyRepPack> future = new CompletableFuture<>();
        try {
            executor.submit(() -> {
                try {
                    for (HotKey hotKey : pack.getList()) {
                        if (!listeners.isEmpty()) {
                            for (HotKeyClientListener listener : listeners) {
                                try {
                                    listener.onHotKey(hotKey);
                                } catch (Exception e) {
                                    logger.error("HotKeyClientListener onHotKey error, hotKey = {}", JSONObject.toJSONString(hotKey), e);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("onNotifyHotKeyPack error", e);
                } finally {
                    future.complete(NotifyHotKeyRepPack.INSTANCE);
                }
            });
        } catch (Exception e) {
            logger.error("submit onNotifyHotKeyPack error", e);
            future.complete(NotifyHotKeyRepPack.INSTANCE);
        }
        return future;
    }

    @Override
    public CompletableFuture<NotifyHotKeyConfigRepPack> onNotifyHotKeyConfigPack(Channel channel, NotifyHotKeyConfigPack pack) {
        CompletableFuture<NotifyHotKeyConfigRepPack> future = new CompletableFuture<>();
        try {
            executor.submit(() -> {
                try {
                    HotKeyConfig config = pack.getConfig();
                    if (!listeners.isEmpty()) {
                        for (HotKeyClientListener listener : listeners) {
                            try {
                                listener.onHotKeyConfig(config);
                            } catch (Exception e) {
                                logger.error("HotKeyClientListener onHotKeyConfig error, hotKey = {}", JSONObject.toJSONString(config), e);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("onNotifyHotKeyConfigPack error", e);
                } finally {
                    future.complete(NotifyHotKeyConfigRepPack.INSTANCE);
                }
            });
        } catch (Exception e) {
            logger.error("submit onNotifyHotKeyConfigPack error", e);
            future.complete(NotifyHotKeyConfigRepPack.INSTANCE);
        }
        return future;
    }
}
