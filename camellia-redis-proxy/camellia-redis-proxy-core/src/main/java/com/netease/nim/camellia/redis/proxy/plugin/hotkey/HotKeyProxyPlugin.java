package com.netease.nim.camellia.redis.proxy.plugin.hotkey;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.plugin.bigkey.DummyBigKeyMonitorCallback;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;

import java.util.List;

/**
 * Created by caojiajun on 2022/9/13
 */
public class HotKeyProxyPlugin implements ProxyPlugin {

    private HotKeyHunterManager manager;

    @Override
    public void init(ProxyBeanFactory factory) {
        String callbackClassName = BeanInitUtils.getClassName("hot.key.monitor.callback", DummyHotKeyMonitorCallback.class.getName());
        Class<?> clazz = BeanInitUtils.parseClass(callbackClassName);
        HotKeyMonitorCallback callback = (HotKeyMonitorCallback) factory.getBean(clazz);
        manager = new HotKeyHunterManager(callback);
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return BuildInProxyPluginEnum.HOT_KEY_PLUGIN.getRequestOrder();
            }

            @Override
            public int reply() {
                return BuildInProxyPluginEnum.HOT_KEY_PLUGIN.getReplyOrder();
            }
        };
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest request) {
        //属于监控类plugin，因此也受isMonitorEnable控制
        if (!ProxyMonitorCollector.isMonitorEnable()) return ProxyPluginResponse.SUCCESS;
        Command command = request.getCommand();
        CommandContext commandContext = command.getCommandContext();
        HotKeyHunter hotKeyHunter = manager.get(commandContext.getBid(), commandContext.getBgroup());
        List<byte[]> keys = command.getKeys();
        hotKeyHunter.incr(keys);
        return ProxyPluginResponse.SUCCESS;
    }
}
