package com.netease.nim.camellia.redis.proxy.plugin.hotkeycache;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;

/**
 * Created by caojiajun on 2022/9/13
 */
public class HotKeyCacheProxyPlugin implements ProxyPlugin {

    private HotKeyCacheManager manager;

    @Override
    public void init(ProxyBeanFactory factory) {
        HotKeyCacheConfig hotKeyCacheConfig = new HotKeyCacheConfig();

        String hotKeyCacheKeyCheckerClassName = ProxyDynamicConf.getString("hot.key.cache.key.checker.className", PrefixMatchHotKeyCacheKeyChecker.class.getName());
        HotKeyCacheKeyChecker hotKeyCacheKeyChecker = (HotKeyCacheKeyChecker) factory.getBean(BeanInitUtils.parseClass(hotKeyCacheKeyCheckerClassName));
        hotKeyCacheConfig.setHotKeyCacheKeyChecker(hotKeyCacheKeyChecker);

        String hotKeyCacheStatsCallbackClassName = ProxyDynamicConf.getString("hot.key.cache.stats.callback.className", DummyHotKeyCacheStatsCallback.class.getName());
        HotKeyCacheStatsCallback hotKeyCacheStatsCallback = (HotKeyCacheStatsCallback) factory.getBean(BeanInitUtils.parseClass(hotKeyCacheStatsCallbackClassName));
        hotKeyCacheConfig.setHotKeyCacheStatsCallback(hotKeyCacheStatsCallback);
        manager = new HotKeyCacheManager(hotKeyCacheConfig);
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return BuildInProxyPluginEnum.HOT_KEY_CACHE_PLUGIN.getRequestOrder();
            }

            @Override
            public int reply() {
                return BuildInProxyPluginEnum.HOT_KEY_CACHE_PLUGIN.getReplyOrder();
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
                HotKeyCache hotKeyCache = manager.get(commandContext.getBid(), commandContext.getBgroup());
                byte[] key = objects[1];
                HotValue value = hotKeyCache.getCache(key);
                if (value != null) {
                    BulkReply bulkReply = new BulkReply(value.getValue());
                    return new ProxyPluginResponse(false, bulkReply);
                }
            }
        }
        return ProxyPluginResponse.SUCCESS;
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
                HotKeyCache hotKeyCache = manager.get(commandContext.getBid(), commandContext.getBgroup());
                byte[] key = command.getObjects()[1];
                byte[] value = ((BulkReply) reply).getRaw();
                hotKeyCache.tryBuildHotKeyCache(key, value);
            }
        }
        return ProxyPluginResponse.SUCCESS;
    }
}
