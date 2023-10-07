package com.netease.nim.camellia.redis.proxy.plugin.hotkey;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyRequest;
import com.netease.nim.camellia.redis.proxy.plugin.rewrite.RouteRewriteResult;
import com.netease.nim.camellia.redis.proxy.plugin.rewrite.RouteRewriteChecker;
import com.netease.nim.camellia.tools.cache.NamespaceCamelliaLocalCache;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.List;

/**
 * Created by caojiajun on 2023/10/7
 */
public class HotKeyRouteRewriteCallback implements HotKeyMonitorCallback, RouteRewriteChecker {

    private final HotKeyMonitorCallback callback;
    private final RouteRewriteChecker routeRewriteChecker;
    private final NamespaceCamelliaLocalCache cache;

    public HotKeyRouteRewriteCallback(HotKeyMonitorCallback callback, RouteRewriteChecker routeRewriteChecker) {
        this.callback = callback;
        this.routeRewriteChecker = routeRewriteChecker;
        int namespaceCapacity = ProxyDynamicConf.getInt("hotkey.route.rewrite.namespace.capacity", 100);
        int capacity = ProxyDynamicConf.getInt("hotkey.route.rewrite.key.capacity", 1000);
        this.cache = new NamespaceCamelliaLocalCache(namespaceCapacity, capacity, false);
    }

    @Override
    public void callback(IdentityInfo identityInfo, List<HotKeyInfo> hotKeys, long checkMillis, long checkThreshold) {
        callback.callback(identityInfo, hotKeys, checkMillis, checkThreshold);
        String namespace = identityInfo.getBid() + "|" + identityInfo.getBgroup();
        long expireMillis = ProxyDynamicConf.getLong("hotkey.route.rewrite.key.expire.millis", identityInfo.getBid(), identityInfo.getBgroup(), 5000L);
        for (HotKeyInfo hotKey : hotKeys) {
            cache.put(namespace, new BytesKey(hotKey.getKey()), true, expireMillis);
        }
    }

    @Override
    public RouteRewriteResult checkRewrite(ProxyRequest request) {
        Command command = request.getCommand();
        //阻塞型命令不支持
        if (command.isBlocking()) {
            return null;
        }
        RedisCommand redisCommand = command.getRedisCommand();
        //发布订阅命令不支持
        if (redisCommand.getCommandType() == RedisCommand.CommandType.PUB_SUB) {
            return null;
        }
        //事务命令不支持
        if (redisCommand.getCommandType() == RedisCommand.CommandType.TRANSACTION) {
            return null;
        }
        //多key的命令不支持
        if (command.getKeys().size() != 1) {
            return null;
        }
        ChannelInfo channelInfo = command.getChannelInfo();
        if (channelInfo == null) {
            return null;
        }
        //事务命令、发布订阅命令不支持
        if (channelInfo.isInTransaction() || channelInfo.isInSubscribe()) {
            return null;
        }
        CommandContext commandContext = command.getCommandContext();
        byte[] key = command.getKeys().get(0);
        String namespace = commandContext.getBid() + "|" + commandContext.getBgroup();
        long ttl = cache.ttl(namespace, new BytesKey(key));
        if (ttl > 0) {
            return routeRewriteChecker.checkRewrite(request);
        }
        return null;
    }
}
