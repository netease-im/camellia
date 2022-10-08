package com.netease.nim.camellia.redis.proxy.netty;


import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.AsyncTaskQueue;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClient;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientAddr;
import com.netease.nim.camellia.redis.proxy.util.BytesKey;
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
    private final AsyncTaskQueue asyncTaskQueue;
    private volatile ConcurrentHashMap<String, RedisClient> redisClientsMapForBlockingCommand;
    private RedisClient bindClient = null;
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
        this.asyncTaskQueue = new AsyncTaskQueue(this);
        this.mock = false;
        this.fromCport = ((InetSocketAddress) ctx.channel().localAddress()).getPort() == GlobalRedisProxyEnv.cport;
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
        if (mock) {
            throw new CamelliaRedisException("mock channel info do not support");
        }
        if (redisClientsMapForBlockingCommand == null) {
            synchronized (this) {
                if (redisClientsMapForBlockingCommand == null) {
                    redisClientsMapForBlockingCommand = new ConcurrentHashMap<>();
                }
            }
        }
        redisClientsMapForBlockingCommand.put(redisClient.getAddr().getUrl(), redisClient);
    }

    public RedisClient tryGetExistsRedisClientForBlockingCommand(RedisClientAddr addr) {
        if (redisClientsMapForBlockingCommand != null && !redisClientsMapForBlockingCommand.isEmpty()) {
            RedisClient client = redisClientsMapForBlockingCommand.get(addr.getUrl());
            if (client != null && client.isValid()) {
                return client;
            }
        }
        return null;
    }

    public ConcurrentHashMap<String, RedisClient> getRedisClientsMapForBlockingCommand() {
        return redisClientsMapForBlockingCommand;
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

    public RedisClient getBindClient() {
        return bindClient;
    }

    public boolean isInTransaction() {
        return inTransaction;
    }

    public void setInTransaction(boolean inTransaction) {
        this.inTransaction = inTransaction;
    }

    public void setBindClient(RedisClient bindClient) {
        this.bindClient = bindClient;
    }

    public void setBindClient(int bindSlot, RedisClient bindClient) {
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
