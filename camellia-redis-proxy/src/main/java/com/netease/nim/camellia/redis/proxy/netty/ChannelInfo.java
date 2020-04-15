package com.netease.nim.camellia.redis.proxy.netty;


import com.netease.nim.camellia.redis.proxy.command.async.AsyncTaskQueue;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public class ChannelInfo {

    private static final AttributeKey<ChannelInfo> ATTRIBUTE_KEY = AttributeKey.valueOf("CI");

    private final String consid;
    private final Map<String, Object> map = new HashMap<>();
    private ChannelStats channelStats = ChannelStats.NO_AUTH;
    private ChannelHandlerContext ctx;
    private AsyncTaskQueue asyncTaskQueue;

    private ChannelInfo(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        this.consid = UUID.randomUUID().toString();

        this.asyncTaskQueue = new AsyncTaskQueue(this);
    }

    /**
     * 初始化ChannelInfo
     */
    public static ChannelInfo init(ChannelHandlerContext ctx) {
        ChannelInfo channelInfo = new ChannelInfo(ctx);
        ctx.channel().attr(ATTRIBUTE_KEY).set(channelInfo);
        return channelInfo;
    }

    /**
     * 获取ChannelInfo
     */
    public static ChannelInfo get(ChannelHandlerContext ctx) {
        if (ctx == null) return null;
        return ctx.channel().attr(ATTRIBUTE_KEY).get();
    }

    public AsyncTaskQueue getAsyncTaskQueue() {
        return asyncTaskQueue;
    }

    public void clear() {
        asyncTaskQueue.clear();
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public String getConsid() {
        return consid;
    }

    public ChannelStats getChannelStats() {
        return channelStats;
    }

    public void setChannelStats(ChannelStats channelStats) {
        this.channelStats = channelStats;
    }

    public void setKV(String key, Object value) {
        map.put(key, value);
    }

    public Object getKV(String key) {
        return map.get(key);
    }

    public boolean containsKV(String key) {
        return map.containsKey(key);
    }

    public static enum ChannelStats {
        AUTH_OK,
        NO_AUTH,
    }
}
