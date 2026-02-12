package com.netease.nim.camellia.redis.proxy.plugin.hotkey;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.plugin.rewrite.DefaultRouteRewriter;
import com.netease.nim.camellia.redis.proxy.plugin.rewrite.RouteRewriter;
import com.netease.nim.camellia.redis.proxy.plugin.rewrite.RouteRewriteResult;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;

import java.util.List;

/**
 * Created by caojiajun on 2023/10/7
 */
public class HotKeyRouteRewriteProxyPlugin implements ProxyPlugin {

    private HotKeyHunterManager manager;
    private HotKeyRouteRewriter rewriter;

    @Override
    public void init(ProxyBeanFactory factory) {
        String rewriteCheckerClassName = BeanInitUtils.getClassName("hot.key.route.rewriter", DefaultRouteRewriter.class.getName());
        RouteRewriter routeRewriter = (RouteRewriter) factory.getBean(BeanInitUtils.parseClass(rewriteCheckerClassName));

        String callbackClassName = BeanInitUtils.getClassName("hot.key.monitor.callback", DummyHotKeyMonitorCallback.class.getName());
        HotKeyMonitorCallback monitorCallback = (HotKeyMonitorCallback) factory.getBean(BeanInitUtils.parseClass(callbackClassName));

        this.rewriter = new HotKeyRouteRewriter(monitorCallback, routeRewriter);
        this.manager = new HotKeyHunterManager(this.rewriter);
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return ProxyPluginEnums.HOT_KEY_ROUTE_REWRITE_PLUGIN.getRequestOrder();
            }

            @Override
            public int reply() {
                return ProxyPluginEnums.HOT_KEY_ROUTE_REWRITE_PLUGIN.getReplyOrder();
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
        RouteRewriteResult result = rewriter.rewrite(request);
        if (result != null) {
            return new ProxyPluginResponse(result);
        }
        return ProxyPluginResponse.SUCCESS;
    }
}
