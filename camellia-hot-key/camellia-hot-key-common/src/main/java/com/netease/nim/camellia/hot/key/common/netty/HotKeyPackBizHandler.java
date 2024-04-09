package com.netease.nim.camellia.hot.key.common.netty;

import com.netease.nim.camellia.hot.key.common.netty.pack.*;
import io.netty.channel.Channel;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/5/9
 */
public interface HotKeyPackBizHandler {

    default CompletableFuture<NotifyHotKeyRepPack> onNotifyHotKeyPack(Channel channel, NotifyHotKeyPack pack) {
        return CompletableFuture.completedFuture(NotifyHotKeyRepPack.INSTANCE);
    }

    default CompletableFuture<NotifyHotKeyConfigRepPack> onNotifyHotKeyConfigPack(Channel channel, NotifyHotKeyConfigPack pack) {
        return CompletableFuture.completedFuture(NotifyHotKeyConfigRepPack.INSTANCE);
    }

    default CompletableFuture<PushRepPack> onPushPack(Channel channel, PushPack pack) {
        return CompletableFuture.completedFuture(PushRepPack.INSTANCE);
    }

    default CompletableFuture<HeartbeatRepPack> onHeartbeatPack(Channel channel, HeartbeatPack pack) {
        return CompletableFuture.completedFuture(HeartbeatRepPack.INSTANCE);
    }

    default CompletableFuture<GetConfigRepPack> onGetConfigPack(Channel channel, GetConfigPack pack) {
        return CompletableFuture.completedFuture(null);
    }

    default CompletableFuture<HotKeyCacheStatsRepPack> onHotKeyCacheStatsPack(Channel channel, HotKeyCacheStatsPack pack) {
        return CompletableFuture.completedFuture(HotKeyCacheStatsRepPack.INSTANCE);
    }

}
