package com.netease.nim.camellia.redis.proxy.hotkey.mointor.plugin;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.hotkey.common.ProxyHotKeyServerDiscovery;
import com.netease.nim.camellia.redis.proxy.hotkey.common.ProxyLocalHotKeyServerDiscovery;
import com.netease.nim.camellia.redis.proxy.hotkey.common.Utils;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;

public class HotKeyPlugin implements ProxyPlugin {


    public static final String HOT_KEY_PLUGIN_ALIAS = "hotKeyPlugin";
    private HotKeyManager hotKeyManager;

    @Override
    public void init(ProxyBeanFactory factory) {
        // 默认使用本地sever
        String hotKeyDiscoveryClassName = ProxyDynamicConf.getString("hot.key.server.discovery.className", ProxyLocalHotKeyServerDiscovery.class.getName());
        ProxyHotKeyServerDiscovery discovery = (ProxyHotKeyServerDiscovery) factory.getBean(BeanInitUtils.parseClass(hotKeyDiscoveryClassName));
        HotKeyMonitorConfig hotKeyMonitorConfig = new HotKeyMonitorConfig();
        hotKeyMonitorConfig.setDiscovery(discovery.getDiscovery());
        hotKeyManager = new HotKeyManager(hotKeyMonitorConfig);
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return Utils.getRequestOrder(HOT_KEY_PLUGIN_ALIAS, 20000);
            }

            @Override
            public int reply() {
                return Utils.getReplyOrder(HOT_KEY_PLUGIN_ALIAS, 0);
            }
        };
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest proxyRequest) {
        //属于监控类plugin，因此也受isMonitorEnable控制
        if (!ProxyMonitorCollector.isMonitorEnable()) return ProxyPluginResponse.SUCCESS;
        Command command = proxyRequest.getCommand();
        RedisCommand redisCommand = command.getRedisCommand();
        CommandContext commandContext = command.getCommandContext();
        HotKeyMonitor hotKeyMonitor = hotKeyManager.getHotKey(commandContext.getBid(), commandContext.getBgroup());
        hotKeyMonitor.push(command.getKeys());
        return ProxyPluginResponse.SUCCESS;
    }
}
