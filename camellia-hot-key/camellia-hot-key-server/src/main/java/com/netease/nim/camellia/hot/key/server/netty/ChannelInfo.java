package com.netease.nim.camellia.hot.key.server.netty;

import com.netease.nim.camellia.hot.key.common.netty.SeqManager;
import com.netease.nim.camellia.hot.key.server.conf.ClientConnectHub;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelInfo {

    private static final AttributeKey<ChannelInfo> ATTRIBUTE_KEY = AttributeKey.valueOf("CI");

    private final String consid;
    private final ChannelHandlerContext ctx;
    private final SeqManager seqManager;
    private final ConcurrentHashMap<String, Boolean> namespaceMap = new ConcurrentHashMap<>();

    private ChannelInfo(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        this.consid = UUID.randomUUID().toString();
        this.seqManager = new SeqManager(ctx.channel());
    }

    /**
     * 初始化ChannelInfo
     * @param ctx ChannelHandlerContext
     * @return ChannelInfo ChannelInfo
     */
    public static ChannelInfo init(ChannelHandlerContext ctx) {
        ChannelInfo channelInfo = new ChannelInfo(ctx);
        ctx.channel().attr(ATTRIBUTE_KEY).set(channelInfo);
        return channelInfo;
    }

    /**
     * 获取ChannelInfo
     * @param ctx ChannelHandlerContext
     * @return ChannelInfo
     */
    public static ChannelInfo get(ChannelHandlerContext ctx) {
        if (ctx == null) return null;
        return ctx.channel().attr(ATTRIBUTE_KEY).get();
    }

    public static ChannelInfo get(Channel channel) {
        if (channel == null) return null;
        return channel.attr(ATTRIBUTE_KEY).get();
    }

    public void addNamespace(String namespace) {
        namespaceMap.put(namespace, true);
        ClientConnectHub.getInstance().updateNamespace(namespace, this);
    }

    public boolean hasNamespace(String namespace) {
        return namespaceMap.containsKey(namespace);
    }

    public Set<String> namespaceSet() {
        return new HashSet<>(namespaceMap.keySet());
    }

    public long genSeqId() {
        return seqManager.genSeqId();
    }

    public String getConsid() {
        return consid;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public SeqManager getSeqManager() {
        return seqManager;
    }
}
