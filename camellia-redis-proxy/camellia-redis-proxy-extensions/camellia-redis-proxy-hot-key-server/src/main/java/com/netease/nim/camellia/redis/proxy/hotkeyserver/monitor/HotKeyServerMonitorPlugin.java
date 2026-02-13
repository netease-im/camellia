package com.netease.nim.camellia.redis.proxy.hotkeyserver.monitor;

import com.netease.nim.camellia.core.discovery.CamelliaDiscoveryFactory;
import com.netease.nim.camellia.core.discovery.LocalConfCamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.LocalConfCamelliaDiscoveryFactory;
import com.netease.nim.camellia.core.discovery.ServerNode;
import com.netease.nim.camellia.core.util.ServerNodeUtils;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeyMonitorSdk;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeySdk;
import com.netease.nim.camellia.hot.key.sdk.ICamelliaHotKeyMonitorSdk;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyMonitorSdkConfig;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeySdkConfig;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.List;

public class HotKeyServerMonitorPlugin implements ProxyPlugin {

    private ICamelliaHotKeyMonitorSdk monitorSdk;

    @Override
    public void init(ProxyBeanFactory factory) {
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

        CamelliaHotKeyMonitorSdkConfig monitorSdkConfig = new CamelliaHotKeyMonitorSdkConfig();

        monitorSdk = new CamelliaHotKeyMonitorSdk(new CamelliaHotKeySdk(config), monitorSdkConfig);
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return ProxyDynamicConf.getInt("hot.key.server.monitor.plugin.request.order",
                        ProxyPluginEnums.HOT_KEY_PLUGIN.getRequestOrder());
            }

            @Override
            public int reply() {
                return ProxyDynamicConf.getInt("hot.key.server.monitor.plugin.reply.order",
                        ProxyPluginEnums.HOT_KEY_PLUGIN.getReplyOrder());
            }
        };
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest proxyRequest) {
        try {
            //属于监控类plugin，因此也受isMonitorEnable控制
            if (!ProxyMonitorCollector.isMonitorEnable()) {
                return ProxyPluginResponse.SUCCESS;
            }
            Command command = proxyRequest.getCommand();
            CommandContext commandContext = command.getCommandContext();
            Long bid = commandContext.getBid();
            String bgroup = commandContext.getBgroup();
            String namespace = ProxyDynamicConf.getString("hot.key.monitor.namespace", bid, bgroup, Utils.getNamespaceOrSetDefault(bid, bgroup));
            List<byte[]> keys = command.getKeys();
            for (byte[] key : keys) {
                String keyStr = Utils.bytesToString(key);
                monitorSdk.push(namespace, keyStr, 1);
            }
            return ProxyPluginResponse.SUCCESS;
        } catch (Exception e) {
            ErrorLogCollector.collect(HotKeyServerMonitorPlugin.class, "hot key error", e);
            return ProxyPluginResponse.SUCCESS;
        }
    }
}
