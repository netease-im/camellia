package com.netease.nim.camellia.hot.key.common.netty;

import com.netease.nim.camellia.hot.key.common.netty.pack.*;
import io.netty.channel.Channel;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/5/9
 */
public interface HotKeyPackBizHandler {

    default CompletableFuture<NotifyHotKeyRepPack> onNotifyHotKeyPack(Channel channel, NotifyHotKeyPack pack) {
        return wrapper(NotifyHotKeyRepPack.INSTANCE);
    }

    default CompletableFuture<NotifyHotKeyConfigRepPack> onNotifyHotKeyConfigPack(Channel channel, NotifyHotKeyConfigPack pack) {
        return wrapper(NotifyHotKeyConfigRepPack.INSTANCE);
    }

    default CompletableFuture<PushRepPack> onPushPack(Channel channel, PushPack pack) {
        return wrapper(PushRepPack.INSTANCE);
    }

    default CompletableFuture<HeartbeatRepPack> onHeartbeatPack(Channel channel, HeartbeatPack pack) {
        return wrapper(HeartbeatRepPack.INSTANCE);
    }

    default CompletableFuture<GetConfigRepPack> onGetConfigPack(Channel channel, GetConfigPack pack) {
        return wrapper(null);
    }

    default CompletableFuture<HotKeyCacheStatsRepPack> onHotKeyCacheStatsPack(Channel channel, HotKeyCacheStatsPack pack) {
        return wrapper(HotKeyCacheStatsRepPack.INSTANCE);
    }

    default <T> CompletableFuture<T> wrapper(T t) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.complete(t);
        return future;
    }
}
