package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.cluster.DefaultProxyClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.cluster.provider.ProxyClusterModeProvider;
import com.netease.nim.camellia.redis.proxy.conf.*;
import com.netease.nim.camellia.redis.proxy.enums.ProxyMode;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.auth.AuthCommandProcessor;
import com.netease.nim.camellia.redis.proxy.monitor.*;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.plugin.DefaultProxyPluginFactory;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPluginInitResp;
import com.netease.nim.camellia.redis.proxy.route.RouteConfProvider;
import com.netease.nim.camellia.redis.proxy.sentinel.DefaultProxySentinelModeProcessor;
import com.netease.nim.camellia.redis.proxy.sentinel.ProxySentinelModeProcessor;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import com.netease.nim.camellia.redis.proxy.util.ConfigInitUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.FastThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class CommandInvoker implements ICommandInvoker {

    private static final Logger logger = LoggerFactory.getLogger(CommandInvoker.class);

    private final IUpstreamClientTemplateFactory factory;
    private final CommandInvokeConfig commandInvokeConfig;

    public CommandInvoker() {

        RouteConfProvider routeConfProvider = ConfigInitUtil.initRouteConfProvider();

        //init ProxyCommandProcessor
        ProxyCommandProcessor proxyCommandProcessor = new ProxyCommandProcessor();
        proxyCommandProcessor.setRouteConfProvider(routeConfProvider);

        //init IUpstreamClientTemplateFactory
        this.factory = ConfigInitUtil.initUpstreamClientTemplateFactory(routeConfProvider);
        GlobalRedisProxyEnv.setClientTemplateFactory(factory);

        //init ProxyClusterModeProcessor/ProxySentinelModeProcessor
        ProxyClusterModeProcessor clusterModeProcessor = null;
        ProxySentinelModeProcessor sentinelModeProcessor = null;
        ProxyMode proxyMode = ServerConf.proxyMode();
        if (proxyMode == ProxyMode.cluster) {
            ProxyClusterModeProvider provider = ConfigInitUtil.initProxyClusterModeProvider();
            clusterModeProcessor = new DefaultProxyClusterModeProcessor(provider);
        } else if (proxyMode == ProxyMode.sentinel) {
            sentinelModeProcessor = new DefaultProxySentinelModeProcessor();
        }

        //init ProxyNodesDiscovery
        proxyCommandProcessor.setProxyNodesDiscovery(ConfigInitUtil.initProxyNodesDiscovery(clusterModeProcessor, sentinelModeProcessor));

        //init monitor
        MonitorCallback monitorCallback = ConfigInitUtil.initMonitorCallback();
        ProxyMonitorCollector.init(monitorCallback);

        //init AuthCommandProcessor
        AuthCommandProcessor authCommandProcessor = new AuthCommandProcessor(routeConfProvider);

        //init plugins
        DefaultProxyPluginFactory proxyPluginFactory = new DefaultProxyPluginFactory();
        ProxyPluginInitResp proxyPluginInitResp = proxyPluginFactory.initPlugins();
        proxyCommandProcessor.updateProxyPluginInitResp(proxyPluginInitResp);
        proxyPluginFactory.registerPluginUpdate(() -> proxyCommandProcessor.updateProxyPluginInitResp(proxyPluginFactory.initPlugins()));

        //init CommandInvokeConfig
        this.commandInvokeConfig = new CommandInvokeConfig(authCommandProcessor, clusterModeProcessor, sentinelModeProcessor, proxyPluginFactory, proxyCommandProcessor);
    }

    @Override
    public CommandInvokeConfig getCommandInvokeConfig() {
        return commandInvokeConfig;
    }

    private static final FastThreadLocal<CommandsRouter> threadLocal = new FastThreadLocal<>();

    @Override
    public void invoke(ChannelHandlerContext ctx, ChannelInfo channelInfo, List<Command> commands) {
        if (commands.isEmpty()) return;
        try {
            CommandsRouter router = threadLocal.get();
            if (router == null) {
                router = new CommandsRouter(factory, commandInvokeConfig);
                logger.info("CommandsRouter init success");
                threadLocal.set(router);
            }
            channelInfo.active(commands);
            router.route(channelInfo, commands);
        } catch (Exception e) {
            ctx.close();
            logger.error(e.getMessage(), e);
        }
    }
}
