package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterModeProvider;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.auth.AuthCommandProcessor;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.monitor.*;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.plugin.DefaultProxyPluginFactory;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;
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

    public CommandInvoker(CamelliaServerProperties serverProperties, CamelliaTranspondProperties transpondProperties) {
        ProxyDynamicConf.updateInitConf(serverProperties.getConfig());

        this.factory = ConfigInitUtil.initUpstreamClientTemplateFactory(serverProperties, transpondProperties);
        GlobalRedisProxyEnv.setClientTemplateFactory(factory);

        MonitorCallback monitorCallback = ConfigInitUtil.initMonitorCallback(serverProperties);
        ProxyMonitorCollector.init(serverProperties, monitorCallback);

        AuthCommandProcessor authCommandProcessor = new AuthCommandProcessor(ConfigInitUtil.initClientAuthProvider(serverProperties));

        ProxyClusterModeProcessor clusterModeProcessor = null;
        if (serverProperties.isClusterModeEnable()) {
            ProxyClusterModeProvider provider = (ProxyClusterModeProvider)serverProperties.getProxyBeanFactory()
                    .getBean(BeanInitUtils.parseClass(serverProperties.getClusterModeProviderClassName()));
            clusterModeProcessor = new ProxyClusterModeProcessor(provider);
        }

        DefaultProxyPluginFactory proxyPluginFactory = new DefaultProxyPluginFactory(serverProperties.getPlugins(), serverProperties.getProxyBeanFactory());
        this.commandInvokeConfig = new CommandInvokeConfig(authCommandProcessor, clusterModeProcessor, proxyPluginFactory);
    }

    private static final FastThreadLocal<CommandsTransponder> threadLocal = new FastThreadLocal<>();

    @Override
    public void invoke(ChannelHandlerContext ctx, ChannelInfo channelInfo, List<Command> commands) {
        if (commands.isEmpty()) return;
        try {
            CommandsTransponder trandponder = threadLocal.get();
            if (trandponder == null) {
                trandponder = new CommandsTransponder(factory, commandInvokeConfig);
                logger.info("CommandsTransponder init success");
                threadLocal.set(trandponder);
            }
            trandponder.transpond(channelInfo, commands);
        } catch (Exception e) {
            ctx.close();
            logger.error(e.getMessage(), e);
        }
    }
}
