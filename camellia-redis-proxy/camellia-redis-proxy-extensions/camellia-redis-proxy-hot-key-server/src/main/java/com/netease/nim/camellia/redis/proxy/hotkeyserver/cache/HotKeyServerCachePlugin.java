package com.netease.nim.camellia.redis.proxy.hotkeyserver.cache;

import com.netease.nim.camellia.core.discovery.CamelliaDiscoveryFactory;
import com.netease.nim.camellia.core.discovery.LocalConfCamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.LocalConfCamelliaDiscoveryFactory;
import com.netease.nim.camellia.core.discovery.ServerNode;
import com.netease.nim.camellia.core.util.ServerNodeUtils;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeyCacheSdk;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeySdk;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyCacheSdkConfig;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeySdkConfig;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.HotKeyCacheKeyChecker;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.HotValue;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.PrefixMatchHotKeyCacheKeyChecker;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

import java.util.List;


public class HotKeyServerCachePlugin implements ProxyPlugin {

    private HotKeyCacheManager hotKeyCacheManager;

    @Override
    public void init(ProxyBeanFactory factory) {
        String hotKeyCacheKeyCheckerClassName = BeanInitUtils.getClassName("hot.key.cache.key.checker", PrefixMatchHotKeyCacheKeyChecker.class.getName());
        HotKeyCacheKeyChecker keyChecker = (HotKeyCacheKeyChecker) factory.getBean(BeanInitUtils.parseClass(hotKeyCacheKeyCheckerClassName));

        String className = BeanInitUtils.getClassName("hot.key.server.discovery", CamelliaDiscoveryFactory.class.getName());
        String name = ProxyDynamicConf.getString("hot.key.server.name", null);

        CamelliaDiscoveryFactory discoveryFactory;
        if (className.equals(LocalConfCamelliaDiscovery.class.getName())) {
            List<ServerNode> list = ServerNodeUtils.parse(ProxyDynamicConf.getString("hot.key.server.list", null));
            discoveryFactory = new LocalConfCamelliaDiscoveryFactory(name, new LocalConfCamelliaDiscovery(list));
        } else {
            discoveryFactory = (CamelliaDiscoveryFactory) factory.getBean(BeanInitUtils.parseClass(className));
        }

        CamelliaHotKeySdkConfig config = new CamelliaHotKeySdkConfig();
        config.setDiscoveryFactory(discoveryFactory);
        config.setServiceName(name);

        CamelliaHotKeyCacheSdkConfig monitorSdkConfig = new CamelliaHotKeyCacheSdkConfig();

        CamelliaHotKeyCacheSdk hotKeyCacheSdk = new CamelliaHotKeyCacheSdk(new CamelliaHotKeySdk(config), monitorSdkConfig);

        hotKeyCacheManager = new HotKeyCacheManager(hotKeyCacheSdk, keyChecker);
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return ProxyDynamicConf.getInt("hot.key.server.cache.plugin.request.order",
                        ProxyPluginEnums.HOT_KEY_CACHE_PLUGIN.getRequestOrder());
            }

            @Override
            public int reply() {
                return ProxyDynamicConf.getInt("hot.key.server.cache.plugin.reply.order",
                        ProxyPluginEnums.HOT_KEY_CACHE_PLUGIN.getReplyOrder());
            }
        };
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest proxyRequest) {
        try {
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
            } else if (redisCommand == RedisCommand.DEL || redisCommand == RedisCommand.SET || redisCommand == RedisCommand.UNLINK ||
                    redisCommand == RedisCommand.SETEX || redisCommand == RedisCommand.SETNX ||
                    redisCommand == RedisCommand.MSET || redisCommand == RedisCommand.MSETNX) {
                tryDeleteCache(command);
            }
            return ProxyPluginResponse.SUCCESS;
        } catch (Exception e) {
            ErrorLogCollector.collect(HotKeyServerCachePlugin.class, "hot key cache request error", e);
            return ProxyPluginResponse.SUCCESS;
        }
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
        try {
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
        } catch (Exception e) {
            ErrorLogCollector.collect(HotKeyServerCachePlugin.class, "hot key cache reply error", e);
            return ProxyPluginResponse.SUCCESS;
        }
    }
}
