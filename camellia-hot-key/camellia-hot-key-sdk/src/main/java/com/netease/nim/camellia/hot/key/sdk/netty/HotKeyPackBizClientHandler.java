package com.netease.nim.camellia.hot.key.sdk.netty;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.hot.key.common.model.HotKey;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyPackBizHandler;
import com.netease.nim.camellia.hot.key.common.netty.pack.NotifyHotKeyConfigPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.NotifyHotKeyConfigRepPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.NotifyHotKeyPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.NotifyHotKeyRepPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyPackBizClientHandler implements HotKeyPackBizHandler {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyPackBizClientHandler.class);

    private final List<HotKeyClientListener> listeners = new ArrayList<>();

    public synchronized void registerListener(HotKeyClientListener listener) {
        listeners.add(listener);
        logger.info("HotKeyClientListener register success, listener = {}", listener);
    }

    @Override
    public NotifyHotKeyRepPack onNotifyHotKeyPack(NotifyHotKeyPack pack) {
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
        return NotifyHotKeyRepPack.INSTANCE;
    }

    @Override
    public NotifyHotKeyConfigRepPack onNotifyHotKeyConfigPack(NotifyHotKeyConfigPack pack) {
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
        return NotifyHotKeyConfigRepPack.INSTANCE;
    }
}
