package com.netease.nim.camellia.hot.key.common.netty;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.hot.key.common.exception.CamelliaHotKeyException;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2023/5/8
 */
public class SeqManager {

    private static final Logger logger = LoggerFactory.getLogger(SeqManager.class);

    private final ConcurrentLinkedHashMap<Long, CompletableFuture<HotKeyPack>> seqMap = new ConcurrentLinkedHashMap.Builder<Long, CompletableFuture<HotKeyPack>>()
            .initialCapacity(HotKeyConstants.Client.sessionCapacity)
            .maximumWeightedCapacity(HotKeyConstants.Client.sessionCapacity)
            .build();

    private Channel channel;
    private final AtomicLong seqIdGen = new AtomicLong(0);

    public SeqManager() {
    }

    public SeqManager(Channel channel) {
        this.channel = channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public long genSeqId() {
        return seqIdGen.incrementAndGet();
    }

    public CompletableFuture<HotKeyPack> putSession(HotKeyPack pack) {
        CompletableFuture<HotKeyPack> future = new CompletableFuture<>();
        seqMap.put(pack.getHeader().getSeqId(), future);
        return future;
    }

    public void complete(HotKeyPack pack) {
        long seqId = pack.getHeader().getSeqId();
        CompletableFuture<HotKeyPack> future = seqMap.remove(seqId);
        if (future != null) {
            future.complete(pack);
        } else {
            logger.warn("unknown seqId = {}, channel = {}", seqId, channel);
        }
    }

    public void clear() {
        for (Map.Entry<Long, CompletableFuture<HotKeyPack>> entry : seqMap.entrySet()) {
            if (channel == null) {
                entry.getValue().completeExceptionally(new CamelliaHotKeyException("channel disconnect"));
            } else {
                entry.getValue().completeExceptionally(new CamelliaHotKeyException(channel + " disconnect"));
            }
        }
        seqMap.clear();
    }
}
