package com.netease.nim.camellia.redis.proxy.hotkey.monitor.plugin;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.hotkey.common.ProxyHotKeyServerDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.hotkey.common.ProxyLocalHotKeyServerDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.hotkey.common.ProxyHotKeyUtils;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;

public class HotKeyMonitorPlugin implements ProxyPlugin {

    private static final String HOT_KEY_PLUGIN_ALIAS = "hotKeyPlugin";

    private HotKeyMonitorManager hotKeyManager;

    @Override
    public void init(ProxyBeanFactory factory) {
        // 默认使用本地sever
        String hotKeyDiscoveryClassName = ProxyDynamicConf.getString("hot.key.server.discovery.className", ProxyLocalHotKeyServerDiscoveryFactory.class.getName());
        ProxyHotKeyServerDiscoveryFactory discovery = (ProxyHotKeyServerDiscoveryFactory) factory.getBean(BeanInitUtils.parseClass(hotKeyDiscoveryClassName));
        HotKeyMonitorConfig hotKeyMonitorConfig = new HotKeyMonitorConfig();
        hotKeyMonitorConfig.setDiscovery(discovery.getDiscovery());
        hotKeyManager = new HotKeyMonitorManager(hotKeyMonitorConfig);
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return ProxyHotKeyUtils.getRequestOrder(HOT_KEY_PLUGIN_ALIAS, 20000);
            }

            @Override
            public int reply() {
                return ProxyHotKeyUtils.getReplyOrder(HOT_KEY_PLUGIN_ALIAS, 0);
            }
        };
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest proxyRequest) {
        //属于监控类plugin，因此也受isMonitorEnable控制
        if (!ProxyMonitorCollector.isMonitorEnable()) {
            return ProxyPluginResponse.SUCCESS;
        }
        Command command = proxyRequest.getCommand();
        CommandContext commandContext = command.getCommandContext();
        HotKeyMonitor hotKeyMonitor = hotKeyManager.getHotKeyMonitor(commandContext.getBid(), commandContext.getBgroup());
        hotKeyMonitor.push(command.getKeys());
        return ProxyPluginResponse.SUCCESS;
    }
}
