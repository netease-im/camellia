package com.netease.nim.camellia.hot.key.server;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.hot.key.common.model.HotKey;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.HotKeyCommand;
import com.netease.nim.camellia.hot.key.common.netty.pack.NotifyHotKeyConfigPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
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
        ConcurrentHashMap<String, ChannelInfo> map = ClientConnectHub.getInstance().getMap();
        HotKeyPack pack = HotKeyPack.newPack(HotKeyCommand.NOTIFY_CONFIG, new NotifyHotKeyConfigPack(hotKeyConfig));
        for (Map.Entry<String, ChannelInfo> entry : map.entrySet()) {
            ChannelInfo channelInfo = entry.getValue();
            if (channelInfo.hasNamespace(namespace)) {
                CompletableFuture<HotKeyPack> future = sendPack(channelInfo, pack);
                future.thenAccept(p -> logger.info("notify hotKeyConfig change success, config = {}", JSONObject.toJSONString(hotKeyConfig)));
            }
        }
    }

    /**
     * 发送热key通知
     * @param hotKeys hotKeys
     */
    public void notifyHotKey(List<HotKey> hotKeys) {
        //todo
    }

    private CompletableFuture<HotKeyPack> sendPack(ChannelInfo channelInfo, HotKeyPack hotKeyPack) {
        hotKeyPack.getHeader().setRequestId(channelInfo.genRequestId());
        CompletableFuture<HotKeyPack> future = channelInfo.getRequestManager().putSession(hotKeyPack);
        channelInfo.getCtx().channel().writeAndFlush(hotKeyPack);
        return future;
    }
}
