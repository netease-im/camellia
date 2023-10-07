package com.netease.nim.camellia.redis.proxy.plugin.hotkey;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.plugin.rewrite.DefaultRouteRewriteChecker;
import com.netease.nim.camellia.redis.proxy.plugin.rewrite.RouteRewriteChecker;
import com.netease.nim.camellia.redis.proxy.plugin.rewrite.RouteRewriteResult;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;

import java.util.List;

/**
 * Created by caojiajun on 2023/10/7
 */
public class HotKeyRouteRewriteProxyPlugin implements ProxyPlugin {

    private HotKeyHunterManager manager;
    private HotKeyRouteRewriteCallback callback;

    @Override
    public void init(ProxyBeanFactory factory) {
        String rewriteCheckerClassName = ProxyDynamicConf.getString("hot.key.route.rewrite.className", DefaultRouteRewriteChecker.class.getName());
        RouteRewriteChecker routeRewriteChecker = (RouteRewriteChecker) factory.getBean(BeanInitUtils.parseClass(rewriteCheckerClassName));

        String callbackClassName = ProxyDynamicConf.getString("hot.key.monitor.callback.className", DummyHotKeyMonitorCallback.class.getName());
        HotKeyMonitorCallback monitorCallback = (HotKeyMonitorCallback) factory.getBean(BeanInitUtils.parseClass(callbackClassName));

        this.callback = new HotKeyRouteRewriteCallback(monitorCallback, routeRewriteChecker);
        this.manager = new HotKeyHunterManager(this.callback);
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return BuildInProxyPluginEnum.HOT_KEY_ROUTE_REWRITE_PLUGIN.getRequestOrder();
            }

            @Override
            public int reply() {
                return BuildInProxyPluginEnum.HOT_KEY_ROUTE_REWRITE_PLUGIN.getReplyOrder();
            }
        };
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest request) {
        //hot key monitor
        Command command = request.getCommand();
        CommandContext commandContext = command.getCommandContext();
        HotKeyHunter hotKeyHunter = manager.get(commandContext.getBid(), commandContext.getBgroup());
        List<byte[]> keys = command.getKeys();
        hotKeyHunter.incr(keys);
        //hot key route rewrite
        RouteRewriteResult result = callback.checkRewrite(request);
        if (result != null) {
            return new ProxyPluginResponse(result);
        }
        return ProxyPluginResponse.SUCCESS;
    }
}
