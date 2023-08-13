package com.netease.nim.camellia.redis.proxy.netty;


import com.netease.nim.camellia.redis.proxy.command.CommandTaskQueue;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.monitor.UpstreamFailMonitor;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.tools.utils.BytesKey;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
    /**
     * 接受命令的队列
     * The queue for accepting commands
     */
    private final CommandTaskQueue commandTaskQueue;
    private volatile ConcurrentHashMap<String, RedisConnection> bindRedisConnectionCache;
    private volatile ConcurrentHashMap<String, RedisConnection> bindSubscribeRedisConnectionCache;
    private RedisConnection bindConnection = null;
    private int bindSlot = -1;
    private boolean inTransaction = false;
    private boolean inSubscribe = false;
    /**
     * 连接代理的客户端地址
     * The client address of the proxy server.
     */
    private final SocketAddress clientSocketAddress;
    private final boolean fromCport;
    private volatile ConcurrentHashMap<BytesKey, Boolean> subscribeChannels;
    private volatile ConcurrentHashMap<BytesKey, Boolean> psubscribeChannels;

    private boolean transactionTag = false;

    private Command cachedMultiCommand;

    private volatile List<Command> cachedCommands;//for transaction commands multi-write

    private long lastCommandMoveTime;

    private String clientName;
    private Long bid;
    private String bgroup;

    private int db = -1;

    public ChannelInfo() {
        this.consid = null;
        this.ctx = null;
        this.clientSocketAddress = null;
        this.commandTaskQueue = null;
        this.mock = true;
        this.fromCport = false;
    }

    private ChannelInfo(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        this.consid = UUID.randomUUID().toString();
        this.clientSocketAddress = ctx.channel().remoteAddress();
        this.commandTaskQueue = new CommandTaskQueue(this);
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

    public CommandTaskQueue getCommandTaskQueue() {
        return commandTaskQueue;
    }

    public void updateBindRedisConnectionCache(RedisConnection redisConnection) {
        if (mock) {
            return;
        }
        if (bindRedisConnectionCache == null) {
            synchronized (this) {
                if (bindRedisConnectionCache == null) {
                    bindRedisConnectionCache = new ConcurrentHashMap<>();
                }
            }
        }
        bindRedisConnectionCache.put(redisConnection.getAddr().getUrl(), redisConnection);
    }

    public RedisConnection tryAcquireBindRedisConnection(RedisConnectionAddr addr) {
        if (mock) {
            return null;
        }
        if (bindRedisConnectionCache != null && !bindRedisConnectionCache.isEmpty()) {
            RedisConnection connection = bindRedisConnectionCache.get(addr.getUrl());
            if (connection != null && connection.isValid()) {
                return connection;
            }
        }
        return null;
    }

    public RedisConnection acquireBindRedisConnection(IUpstreamClient upstreamClient, RedisConnectionAddr addr) {
        if (mock) {
            return null;
        }
        if (bindRedisConnectionCache != null && !bindRedisConnectionCache.isEmpty()) {
            RedisConnection connection = bindRedisConnectionCache.get(addr.getUrl());
            if (connection != null && connection.isValid()) {
                connection.stopIdleCheck();
                return connection;
            }
        }
        RedisConnection connection = RedisConnectionHub.getInstance().newConnection(upstreamClient, addr);
        if (connection == null || !connection.isValid()) return null;
        if (bindRedisConnectionCache == null) {
            synchronized (this) {
                if (bindRedisConnectionCache == null) {
                    bindRedisConnectionCache = new ConcurrentHashMap<>();
                }
            }
        }
        bindRedisConnectionCache.put(addr.getUrl(), connection);
        return connection;
    }

    public RedisConnection acquireBindSubscribeRedisConnection(IUpstreamClient upstreamClient, RedisConnectionAddr addr) {
        if (mock) {
            return null;
        }
        if (bindSubscribeRedisConnectionCache != null && !bindSubscribeRedisConnectionCache.isEmpty()) {
            RedisConnection connection = bindSubscribeRedisConnectionCache.get(addr.getUrl());
            if (connection != null && connection.isValid()) {
                connection.stopIdleCheck();
                return connection;
            }
        }
        RedisConnection connection = RedisConnectionHub.getInstance().newConnection(upstreamClient, addr);
        if (connection == null || !connection.isValid()) return null;
        if (bindSubscribeRedisConnectionCache == null) {
            synchronized (this) {
                if (bindSubscribeRedisConnectionCache == null) {
                    bindSubscribeRedisConnectionCache = new ConcurrentHashMap<>();
                }
            }
        }
        bindSubscribeRedisConnectionCache.put(addr.getUrl(), connection);
        return connection;
    }

    public ConcurrentHashMap<String, RedisConnection> getBindSubscribeRedisConnectionCache() {
        return bindSubscribeRedisConnectionCache;
    }

    public ConcurrentHashMap<String, RedisConnection> getBindRedisConnectionCache() {
        return bindRedisConnectionCache;
    }

    public void clear() {
        commandTaskQueue.clear();
        inSubscribe = false;
        inTransaction = false;
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

    public RedisConnection getBindConnection() {
        return bindConnection;
    }

    public boolean isInTransaction() {
        return inTransaction;
    }

    public void setInTransaction(boolean inTransaction) {
        this.inTransaction = inTransaction;
        if (!inTransaction) {
            this.transactionTag = false;
        }
    }

    public void setBindConnection(RedisConnection bindConnection) {
        this.bindConnection = bindConnection;
    }

    public void setBindConnection(int bindSlot, RedisConnection bindConnection) {
        if (bindSlot >= 0 && bindConnection == null) {
            return;
        }
        this.bindConnection = bindConnection;
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

    public void addInTransactionCommands(Command command) {
        if (cachedCommands == null) {
            synchronized (this) {
                if (cachedCommands == null) {
                    cachedCommands = new ArrayList<>();
                }
            }
        }
        if (command.isBlocking()) {
            ErrorLogCollector.collect(ChannelInfo.class, "blocking command do not support transaction multi-write");
            return;
        }
        cachedCommands.add(command);
    }

    public void flushInTransactionCommands(int db, IUpstreamClient upstreamClient) {
        if (upstreamClient != null && !cachedCommands.isEmpty()) {
            List<CompletableFuture<Reply>> futureList = new ArrayList<>();
            for (Command command : cachedCommands) {
                CompletableFuture<Reply> future = new CompletableFuture<>();
                if (ProxyMonitorCollector.isMonitorEnable()) {
                    UpstreamFailMonitor.stats(upstreamClient.getResource().getUrl(), command, future);
                }
                futureList.add(future);
            }
            upstreamClient.sendCommand(db, cachedCommands, futureList);
        }
    }

    public void clearInTransactionCommands() {
        if (cachedCommands != null) {
            cachedCommands.clear();
        }
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

    public int getDb() {
        return db;
    }

    public void setDb(int db) {
        this.db = db;
    }

    public boolean isTransactionTag() {
        return transactionTag;
    }

    public void setTransactionTag(boolean transactionTag) {
        this.transactionTag = transactionTag;
    }

    public static enum ChannelStats {
        AUTH_OK,
        NO_AUTH,
    }
}
