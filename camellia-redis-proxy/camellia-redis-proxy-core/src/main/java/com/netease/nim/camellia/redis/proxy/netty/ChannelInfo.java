package com.netease.nim.camellia.redis.proxy.netty;


import com.netease.nim.camellia.redis.proxy.command.CommandTaskQueue;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.tools.utils.BytesKey;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public class ChannelInfo {

    private static final AttributeKey<ChannelInfo> ATTRIBUTE_KEY = AttributeKey.valueOf("CI");

    private final boolean mock;

    private final String consid;
    private ChannelStats channelStats = ChannelStats.NO_AUTH;
    private final ChannelHandlerContext ctx;
    private final CommandTaskQueue asyncTaskQueue;
    private volatile ConcurrentHashMap<String, RedisConnection> bindRedisClientCache;
    private RedisConnection bindClient = null;
    private int bindSlot = -1;
    private boolean inTransaction = false;
    private boolean inSubscribe = false;
    private final SocketAddress clientSocketAddress;
    private final boolean fromCport;
    private volatile ConcurrentHashMap<BytesKey, Boolean> subscribeChannels;
    private volatile ConcurrentHashMap<BytesKey, Boolean> psubscribeChannels;
    private Command cachedMultiCommand;

    private long lastCommandMoveTime;

    private String clientName;
    private Long bid;
    private String bgroup;

    public ChannelInfo() {
        this.consid = null;
        this.ctx = null;
        this.clientSocketAddress = null;
        this.asyncTaskQueue = null;
        this.mock = true;
        this.fromCport = false;
    }

    private ChannelInfo(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        this.consid = UUID.randomUUID().toString();
        this.clientSocketAddress = ctx.channel().remoteAddress();
        this.asyncTaskQueue = new CommandTaskQueue(this);
        this.mock = false;
        this.fromCport = ((InetSocketAddress) ctx.channel().localAddress()).getPort() == GlobalRedisProxyEnv.getCport();
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

    public CommandTaskQueue getAsyncTaskQueue() {
        return asyncTaskQueue;
    }

    public void updateBindRedisClientCache(RedisConnection redisConnection) {
        if (mock) {
            return;
        }
        if (bindRedisClientCache == null) {
            synchronized (this) {
                if (bindRedisClientCache == null) {
                    bindRedisClientCache = new ConcurrentHashMap<>();
                }
            }
        }
        bindRedisClientCache.put(redisConnection.getAddr().getUrl(), redisConnection);
    }

    public RedisConnection tryAcquireBindRedisClient(RedisConnectionAddr addr) {
        if (mock) {
            return null;
        }
        if (bindRedisClientCache != null && !bindRedisClientCache.isEmpty()) {
            RedisConnection client = bindRedisClientCache.get(addr.getUrl());
            if (client != null && client.isValid()) {
                return client;
            }
        }
        return null;
    }

    public RedisConnection acquireBindRedisClient(RedisConnectionAddr addr) {
        if (mock) {
            return null;
        }
        if (bindRedisClientCache != null && !bindRedisClientCache.isEmpty()) {
            RedisConnection client = bindRedisClientCache.get(addr.getUrl());
            if (client != null && client.isValid()) {
                client.stopIdleCheck();
                return client;
            }
        }
        RedisConnection client = RedisConnectionHub.getInstance().newConnection(addr);
        if (client == null) return null;
        if (bindRedisClientCache == null) {
            synchronized (this) {
                if (bindRedisClientCache == null) {
                    bindRedisClientCache = new ConcurrentHashMap<>();
                }
            }
        }
        bindRedisClientCache.put(addr.getUrl(), client);
        return client;
    }

    public ConcurrentHashMap<String, RedisConnection> getBindRedisClientCache() {
        return bindRedisClientCache;
    }

    public void clear() {
        asyncTaskQueue.clear();
        inSubscribe = false;
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

    public RedisConnection getBindClient() {
        return bindClient;
    }

    public boolean isInTransaction() {
        return inTransaction;
    }

    public void setInTransaction(boolean inTransaction) {
        this.inTransaction = inTransaction;
    }

    public void setBindClient(RedisConnection bindClient) {
        this.bindClient = bindClient;
    }

    public void setBindClient(int bindSlot, RedisConnection bindClient) {
        if (bindSlot >= 0 && bindClient == null) {
            return;
        }
        this.bindClient = bindClient;
        this.bindSlot = bindSlot;
    }

    public int getBindSlot() {
        return bindSlot;
    }

    public void updateCachedMultiCommand(Command command) {
        this.cachedMultiCommand = command;
    }

    public Command getCachedMultiCommand() {
        return this.cachedMultiCommand;
    }

    public void addSubscribeChannels(byte[]...channels) {
        if (subscribeChannels == null) {
            synchronized (this) {
                if (subscribeChannels == null) {
                    subscribeChannels = new ConcurrentHashMap<>();
                }
            }
        }
        if (channels != null) {
            for (byte[] channel : channels) {
                subscribeChannels.put(new BytesKey(channel), true);
            }
        }
    }

    public void removeSubscribeChannels(byte[]...channels) {
        if (subscribeChannels != null && channels != null) {
            for (byte[] channel : channels) {
                subscribeChannels.remove(new BytesKey(channel));
            }
        }
    }

    public void addPSubscribeChannels(byte[]...channels) {
        if (psubscribeChannels == null) {
            synchronized (this) {
                if (psubscribeChannels == null) {
                    psubscribeChannels = new ConcurrentHashMap<>();
                }
            }
        }
        if (channels != null) {
            for (byte[] channel : channels) {
                psubscribeChannels.put(new BytesKey(channel), true);
            }
        }
    }

    public void removePSubscribeChannels(byte[]...channels) {
        if (psubscribeChannels != null && channels != null) {
            for (byte[] channel : channels) {
                psubscribeChannels.remove(new BytesKey(channel));
            }
        }
    }

    public boolean hasSubscribeChannels() {
        if (subscribeChannels != null && !subscribeChannels.isEmpty())  {
            return true;
        }
        return psubscribeChannels != null && !psubscribeChannels.isEmpty();
    }

    public boolean isInSubscribe() {
        return inSubscribe;
    }

    public void setInSubscribe(boolean inSubscribe) {
        this.inSubscribe = inSubscribe;
    }

    public long getLastCommandMoveTime() {
        return lastCommandMoveTime;
    }

    public void setLastCommandMoveTime(long lastCommandMoveTime) {
        this.lastCommandMoveTime = lastCommandMoveTime;
    }

    public boolean isFromCport() {
        return fromCport;
    }

    public static enum ChannelStats {
        AUTH_OK,
        NO_AUTH,
    }
}
