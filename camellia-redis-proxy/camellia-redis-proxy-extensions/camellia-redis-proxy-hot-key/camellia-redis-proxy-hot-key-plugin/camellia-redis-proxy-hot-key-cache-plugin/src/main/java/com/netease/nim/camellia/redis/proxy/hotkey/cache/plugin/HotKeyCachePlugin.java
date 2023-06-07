package com.netease.nim.camellia.redis.proxy.hotkey.cache.plugin;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.hotkey.common.ProxyHotKeyServerDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.hotkey.common.ProxyLocalHotKeyServerDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.hotkey.common.ProxyHotKeyUtils;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.HotKeyCacheKeyChecker;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.HotValue;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.PrefixMatchHotKeyCacheKeyChecker;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;

public class HotKeyCachePlugin implements ProxyPlugin {

    private static final String HOT_KEY_CACHE_PLUGIN_ALIAS = "hotKeyCachePlugin";

    private HotKeyCacheManager hotKeyCacheManager;

    @Override
    public void init(ProxyBeanFactory factory) {
        // 默认使用本地sever
        String hotKeyCacheDiscoveryClassName = ProxyDynamicConf.getString("hot.key.server.discovery.className", ProxyLocalHotKeyServerDiscoveryFactory.class.getName());
        ProxyHotKeyServerDiscoveryFactory discoveryFactory = (ProxyHotKeyServerDiscoveryFactory) factory.getBean(BeanInitUtils.parseClass(hotKeyCacheDiscoveryClassName));
        HotKeyCacheConfig hotKeyCacheConfig = new HotKeyCacheConfig();
        hotKeyCacheConfig.setDiscovery(discoveryFactory.getDiscovery());

        String hotKeyCacheKeyCheckerClassName = ProxyDynamicConf.getString("hot.key.cache.key.checker.className", PrefixMatchHotKeyCacheKeyChecker.class.getName());
        HotKeyCacheKeyChecker hotKeyCacheKeyChecker = (HotKeyCacheKeyChecker) factory.getBean(BeanInitUtils.parseClass(hotKeyCacheKeyCheckerClassName));
        hotKeyCacheConfig.setHotKeyCacheKeyChecker(hotKeyCacheKeyChecker);

        hotKeyCacheManager = new HotKeyCacheManager(hotKeyCacheConfig);
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return ProxyHotKeyUtils.getRequestOrder(HOT_KEY_CACHE_PLUGIN_ALIAS, 10000);
            }

            @Override
            public int reply() {
                return ProxyHotKeyUtils.getReplyOrder(HOT_KEY_CACHE_PLUGIN_ALIAS, Integer.MIN_VALUE + 10000);
            }
        };
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest proxyRequest) {
        Command command = proxyRequest.getCommand();
        RedisCommand redisCommand = command.getRedisCommand();
        // 只对get命令做缓存
        if (redisCommand == RedisCommand.GET) {
            byte[][] objects = command.getObjects();
            if (objects.length > 1) {
                CommandContext commandContext = command.getCommandContext();
                HotKeyCache hotKeyCache = hotKeyCacheManager.getHotKeyCache(commandContext.getBid(), commandContext.getBgroup());
                byte[] key = objects[1];
                HotValue value = hotKeyCache.getCache(key);
                if (value != null) {
                    BulkReply bulkReply = new BulkReply(value.getValue());
                    return new ProxyPluginResponse(false, bulkReply);
                }
            }
        } else if (redisCommand == RedisCommand.DEL || redisCommand == RedisCommand.SET ||
                redisCommand == RedisCommand.SETEX || redisCommand == RedisCommand.SETNX ||
                redisCommand == RedisCommand.MSET || redisCommand == RedisCommand.MSETNX) {
            tryDeleteCache(command);
        }
        return ProxyPluginResponse.SUCCESS;
    }

    private void tryDeleteCache(Command command) {
        CommandContext commandContext = command.getCommandContext();
        HotKeyCache hotKeyCache = hotKeyCacheManager.getHotKeyCache(commandContext.getBid(), commandContext.getBgroup());
        for (byte[] key : command.getKeys()) {
            if (hotKeyCache.isHotKey(key)) {
                // 删除key
                hotKeyCache.delCache(key);
            }
        }
    }

    @Override
    public ProxyPluginResponse executeReply(ProxyReply proxyReply) {
        if (proxyReply.isFromPlugin()) return ProxyPluginResponse.SUCCESS;
        Command command = proxyReply.getCommand();
        if (command == null) return ProxyPluginResponse.SUCCESS;
        RedisCommand redisCommand = command.getRedisCommand();
        if (redisCommand == RedisCommand.GET) {
            Reply reply = proxyReply.getReply();
            if (reply instanceof BulkReply) {
                CommandContext commandContext = proxyReply.getCommandContext();
                HotKeyCache hotKeyCache = hotKeyCacheManager.getHotKeyCache(commandContext.getBid(), commandContext.getBgroup());
                byte[] key = command.getObjects()[1];
                byte[] value = ((BulkReply) reply).getRaw();
                hotKeyCache.tryBuildHotKeyCache(key, value);
            }
        }
        return ProxyPluginResponse.SUCCESS;
    }
}
