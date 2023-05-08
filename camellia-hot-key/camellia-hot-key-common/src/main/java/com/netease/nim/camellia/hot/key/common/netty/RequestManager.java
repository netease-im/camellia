package com.netease.nim.camellia.hot.key.common.netty;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/5/8
 */
public class RequestManager {

    private static final Logger logger = LoggerFactory.getLogger(RequestManager.class);

    private final ConcurrentLinkedHashMap<Long, CompletableFuture<HotKeyPack>> requests = new ConcurrentLinkedHashMap.Builder<Long, CompletableFuture<HotKeyPack>>()
            .initialCapacity(100000)
            .maximumWeightedCapacity(100000)
            .build();

    private final Channel channel;

    public RequestManager(Channel channel) {
        this.channel = channel;
    }

    public CompletableFuture<HotKeyPack> putSession(HotKeyPack pack) {
        CompletableFuture<HotKeyPack> future = new CompletableFuture<>();
        requests.put(pack.getHeader().getRequestId(), future);
        return future;
    }

    public void complete(HotKeyPack pack) {
        long requestId = pack.getHeader().getRequestId();
        CompletableFuture<HotKeyPack> future = requests.get(requestId);
        if (future != null) {
            future.complete(pack);
        } else {
            logger.warn("unknown requestId = {}, channel = {}", requestId, channel);
        }
    }
}
