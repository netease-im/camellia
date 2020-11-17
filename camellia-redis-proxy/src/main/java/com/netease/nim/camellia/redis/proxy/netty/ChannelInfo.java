package com.netease.nim.camellia.redis.proxy.netty;


import com.netease.nim.camellia.redis.proxy.command.async.AsyncTaskQueue;
import com.netease.nim.camellia.redis.proxy.command.async.RedisClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.net.SocketAddress;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public class ChannelInfo {

    private static final AttributeKey<ChannelInfo> ATTRIBUTE_KEY = AttributeKey.valueOf("CI");

    private final String consid;
    private ChannelStats channelStats = ChannelStats.NO_AUTH;
    private final ChannelHandlerContext ctx;
    private final AsyncTaskQueue asyncTaskQueue;
    private LinkedBlockingQueue<RedisClient> redisClientsForBlockingCommand = null;
    private RedisClient bindClient = null;
    private final SocketAddress clientSocketAddress;

    private String clientName;
    private Long bid;
    private String bgroup;

    private ChannelInfo(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        this.consid = UUID.randomUUID().toString();
        this.clientSocketAddress = ctx.channel().remoteAddress();
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

    public void addRedisClientForBlockingCommand(RedisClient redisClient) {
        if (redisClientsForBlockingCommand == null) {
            synchronized (this) {
                if (redisClientsForBlockingCommand == null) {
                    redisClientsForBlockingCommand = new LinkedBlockingQueue<>(100);
                }
            }
        }
        boolean offer = redisClientsForBlockingCommand.offer(redisClient);
        while (!offer) {
            redisClientsForBlockingCommand.poll();
            offer = redisClientsForBlockingCommand.offer(redisClient);
        }
    }

    public Queue<RedisClient> getRedisClientsForBlockingCommand() {
        return redisClientsForBlockingCommand;
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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Long getBid() {
        return bid;
    }

    public void setBid(Long bid) {
        this.bid = bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    public SocketAddress getClientSocketAddress() {
        return clientSocketAddress;
    }

    public void setBgroup(String bgroup) {
        this.bgroup = bgroup;
    }

    public RedisClient getBindClient() {
        return bindClient;
    }

    public void setBindClient(RedisClient bindClient) {
        this.bindClient = bindClient;
    }

    public static enum ChannelStats {
        AUTH_OK,
        NO_AUTH,
    }
}
