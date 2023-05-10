package com.netease.nim.camellia.hot.key.common.netty;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.hot.key.common.exception.CamelliaHotKeyException;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/5/8
 */
public class RequestManager {

    private static final Logger logger = LoggerFactory.getLogger(RequestManager.class);

    private final ConcurrentLinkedHashMap<Long, CompletableFuture<HotKeyPack>> requests = new ConcurrentLinkedHashMap.Builder<Long, CompletableFuture<HotKeyPack>>()
            .initialCapacity(HotKeyConstants.Client.sessionCapacity)
            .maximumWeightedCapacity(HotKeyConstants.Client.sessionCapacity)
            .build();

    private Channel channel;

    public RequestManager() {
    }

    public RequestManager(Channel channel) {
        this.channel = channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public CompletableFuture<HotKeyPack> putSession(HotKeyPack pack) {
        CompletableFuture<HotKeyPack> future = new CompletableFuture<>();
        requests.put(pack.getHeader().getRequestId(), future);
        return future;
    }

    public void complete(HotKeyPack pack) {
        long requestId = pack.getHeader().getRequestId();
        CompletableFuture<HotKeyPack> future = requests.remove(requestId);
        if (future != null) {
            future.complete(pack);
        } else {
            logger.warn("unknown requestId = {}, channel = {}", requestId, channel);
        }
    }

    public void clear() {
        for (Map.Entry<Long, CompletableFuture<HotKeyPack>> entry : requests.entrySet()) {
            if (channel == null) {
                entry.getValue().completeExceptionally(new CamelliaHotKeyException("channel disconnect"));
            } else {
                entry.getValue().completeExceptionally(new CamelliaHotKeyException(channel + " disconnect"));
            }
        }
    }
}
