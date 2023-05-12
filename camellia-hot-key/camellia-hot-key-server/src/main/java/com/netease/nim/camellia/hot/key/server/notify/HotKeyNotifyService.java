package com.netease.nim.camellia.hot.key.server.notify;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.hot.key.common.model.HotKey;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.HotKeyCommand;
import com.netease.nim.camellia.hot.key.common.netty.pack.NotifyHotKeyConfigPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.NotifyHotKeyPack;
import com.netease.nim.camellia.hot.key.server.conf.CacheableHotKeyConfigService;
import com.netease.nim.camellia.hot.key.server.conf.ClientConnectHub;
import com.netease.nim.camellia.hot.key.server.netty.ChannelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyNotifyService {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyNotifyService.class);
    private final CacheableHotKeyConfigService hotKeyConfigService;

    public HotKeyNotifyService(CacheableHotKeyConfigService hotKeyConfigService) {
        this.hotKeyConfigService = hotKeyConfigService;
    }

    /**
     * 发送通知给客户端
     * @param namespace namespace
     */
    public void notifyHotKeyNotifyChange(String namespace) {
        HotKeyConfig hotKeyConfig = hotKeyConfigService.get(namespace);
        HotKeyPack pack = HotKeyPack.newPack(HotKeyCommand.NOTIFY_CONFIG, new NotifyHotKeyConfigPack(hotKeyConfig));

        ConcurrentHashMap<String, Boolean> map = ClientConnectHub.getInstance().getMap(namespace);
        if (map == null) return;
        for (String consid : map.keySet()) {
            ChannelInfo channelInfo = ClientConnectHub.getInstance().get(consid);
            if (channelInfo != null) {
                CompletableFuture<HotKeyPack> future = sendPack(channelInfo, pack);
                future.thenAccept(p -> {
                    if (p.getHeader().isEmptyBody()) {
                        logger.error("notify HotKeyConfig change fail, channel = {}, config = {}",
                                channelInfo.getCtx().channel(), JSONObject.toJSONString(hotKeyConfig));
                    } else {
                        logger.info("notify HotKeyConfig change success, channel = {}, config = {}",
                                channelInfo.getCtx().channel(), JSONObject.toJSONString(hotKeyConfig));
                    }
                });
                future.exceptionally(throwable -> {
                    logger.error("notify HotKeyConfig change error, channel = {}, config = {}",
                            channelInfo.getCtx().channel(), JSONObject.toJSONString(hotKeyConfig), throwable);
                    return null;
                });
            }
        }
    }

    /**
     * 发送热key通知
     * @param hotKey hotKey
     */
    public void notifyHotKey(HotKey hotKey) {
        HotKeyPack pack = HotKeyPack.newPack(HotKeyCommand.NOTIFY_HOTKEY, new NotifyHotKeyPack(Collections.singletonList(hotKey)));
        ConcurrentHashMap<String, Boolean> map = ClientConnectHub.getInstance().getMap(hotKey.getNamespace());
        if (map == null || map.isEmpty()) {
            return;
        }
        for (String consid : map.keySet()) {
            ChannelInfo channelInfo = ClientConnectHub.getInstance().get(consid);
            if (channelInfo != null) {
                CompletableFuture<HotKeyPack> future = sendPack(channelInfo, pack);

                future.thenAccept(p -> {
                    if (p.getHeader().isEmptyBody()) {
                        logger.error("notify HotKey change fail, channel = {}, hotKey = {}",
                                channelInfo.getCtx().channel(), JSONObject.toJSONString(hotKey));
                    } else {
                        logger.info("notify HotKey change success, channel = {}, hotKey = {}",
                                channelInfo.getCtx().channel(), JSONObject.toJSONString(hotKey));
                    }
                });
                future.exceptionally(throwable -> {
                    logger.error("notify HotKey change error, channel = {}, hotKey = {}",
                            channelInfo.getCtx().channel(), JSONObject.toJSONString(hotKey), throwable);
                    return null;
                });
            }
        }
    }

    private CompletableFuture<HotKeyPack> sendPack(ChannelInfo channelInfo, HotKeyPack hotKeyPack) {
        hotKeyPack.getHeader().setSeqId(channelInfo.genSeqId());
        CompletableFuture<HotKeyPack> future = channelInfo.getSeqManager().putSession(hotKeyPack);
        channelInfo.getCtx().channel().writeAndFlush(hotKeyPack);
        return future;
    }
}
