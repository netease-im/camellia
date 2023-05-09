package com.netease.nim.camellia.hot.key.server;


import com.netease.nim.camellia.hot.key.common.netty.RequestManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ChannelInfo {

    private static final AttributeKey<ChannelInfo> ATTRIBUTE_KEY = AttributeKey.valueOf("CI");

    private final String consid;
    private final ChannelHandlerContext ctx;
    private final RequestManager requestManager;
    private final ConcurrentHashMap<String, Boolean> namespaceMap = new ConcurrentHashMap<>();
    private final AtomicLong requestIdGen = new AtomicLong(0);

    private ChannelInfo(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        this.consid = UUID.randomUUID().toString();
        this.requestManager = new RequestManager(ctx.channel().remoteAddress());
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
    }

    public boolean hasNamespace(String namespace) {
        return namespaceMap.containsKey(namespace);
    }

    public long genRequestId() {
        return requestIdGen.incrementAndGet();
    }

    public String getConsid() {
        return consid;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }
}
