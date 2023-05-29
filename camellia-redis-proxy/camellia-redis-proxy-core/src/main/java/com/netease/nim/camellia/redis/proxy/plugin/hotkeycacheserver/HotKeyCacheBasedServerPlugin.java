package com.netease.nim.camellia.redis.proxy.plugin.hotkeycacheserver;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.*;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;

public class HotKeyCacheBasedServerPlugin implements ProxyPlugin {

    @Override
    public void init(ProxyBeanFactory factory) {
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return BuildInProxyPluginEnum.HOT_KEY_CACHE_BASED_SERVER_PLUGIN.getRequestOrder();
            }

            @Override
            public int reply() {
                return BuildInProxyPluginEnum.HOT_KEY_CACHE_BASED_SERVER_PLUGIN.getRequestOrder();
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
                HotKeyCacheBasedServer hotKeyCache = HotKeyCacheBasedServerManager.getHotKeyCache(commandContext.getBid(), commandContext.getBgroup());
                byte[] key = objects[1];
                HotValue value = hotKeyCache.getCache(key);
                if (value != null) {

                    BulkReply bulkReply = new BulkReply(value.getValue());
                    return new ProxyPluginResponse(false, bulkReply);
                }
            }
            // 如果是del 和 set 命令，需要对cache进行去除
        } else if (redisCommand == RedisCommand.DEL || redisCommand == RedisCommand.SET) {
            tryDeleteCache(command);
        }
        return ProxyPluginResponse.SUCCESS;
    }

    private void tryDeleteCache(Command command) {
        byte[][] objects = command.getObjects();
        if (objects.length > 1) {
            CommandContext commandContext = command.getCommandContext();
            HotKeyCacheBasedServer hotKeyCache = HotKeyCacheBasedServerManager.getHotKeyCache(commandContext.getBid(), commandContext.getBgroup());
            byte[] key = objects[1];
            if (hotKeyCache.check(key)) {
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
                HotKeyCacheBasedServer hotKeyCache = HotKeyCacheBasedServerManager.getHotKeyCache(commandContext.getBid(), commandContext.getBgroup());
                byte[] key = command.getObjects()[1];
                byte[] value = ((BulkReply) reply).getRaw();
                hotKeyCache.tryBuildHotKeyCache(key, value);
            }
        }
        return ProxyPluginResponse.SUCCESS;
    }
}
