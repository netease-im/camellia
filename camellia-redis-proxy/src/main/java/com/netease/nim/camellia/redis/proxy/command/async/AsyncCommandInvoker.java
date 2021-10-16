package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.command.*;
import com.netease.nim.camellia.redis.proxy.command.async.bigkey.BigKeyHunter;
import com.netease.nim.camellia.redis.proxy.command.async.bigkey.CommandBigKeyMonitorConfig;
import com.netease.nim.camellia.redis.proxy.command.async.converter.ConverterConfig;
import com.netease.nim.camellia.redis.proxy.command.async.converter.Converters;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.CommandHotKeyMonitorConfig;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyHunterManager;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.CommandHotKeyCacheConfig;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotKeyCacheManager;
import com.netease.nim.camellia.redis.proxy.command.async.interceptor.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.CommandSpendTimeConfig;
import com.netease.nim.camellia.redis.proxy.command.AuthCommandProcessor;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.monitor.*;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
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
public class AsyncCommandInvoker implements CommandInvoker {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCommandInvoker.class);

    private final AsyncCamelliaRedisTemplateChooser chooser;
    private final CommandInvokeConfig commandInvokeConfig;

    public AsyncCommandInvoker(CamelliaServerProperties serverProperties, CamelliaTranspondProperties transpondProperties) {
        PasswordMaskUtils.maskEnable = serverProperties.isMonitorDataMaskPassword();
        this.chooser = new AsyncCamelliaRedisTemplateChooser(transpondProperties);

        if (serverProperties.isMonitorEnable()) {
            MonitorCallback monitorCallback = ConfigInitUtil.initMonitorCallback(serverProperties);
            RedisMonitor.init(serverProperties.getMonitorIntervalSeconds(), serverProperties.isCommandSpendTimeMonitorEnable(), monitorCallback);
        }

        int monitorIntervalSeconds = serverProperties.getMonitorIntervalSeconds();
        CommandInterceptor commandInterceptor = ConfigInitUtil.initCommandInterceptor(serverProperties);
        CommandSpendTimeConfig commandSpendTimeConfig = ConfigInitUtil.initCommandSpendTimeConfig(serverProperties);
        if (commandSpendTimeConfig != null) {
            SlowCommandMonitor.init(monitorIntervalSeconds);
        }

        HotKeyHunterManager hotKeyHunterManager = null;
        CommandHotKeyMonitorConfig commandHotKeyMonitorConfig = ConfigInitUtil.initCommandHotKeyMonitorConfig(serverProperties);
        if (commandHotKeyMonitorConfig != null) {
            HotKeyMonitor.init(monitorIntervalSeconds);
            hotKeyHunterManager = new HotKeyHunterManager(commandHotKeyMonitorConfig);
        }

        HotKeyCacheManager hotKeyCacheManager = null;
        CommandHotKeyCacheConfig commandHotKeyCacheConfig = ConfigInitUtil.initHotKeyCacheConfig(serverProperties);
        if (commandHotKeyCacheConfig != null) {
            HotKeyCacheMonitor.init(monitorIntervalSeconds);
            hotKeyCacheManager = new HotKeyCacheManager(commandHotKeyCacheConfig);
        }

        BigKeyHunter bigKeyHunter = null;
        CommandBigKeyMonitorConfig commandBigKeyMonitorConfig = ConfigInitUtil.initBigKeyMonitorConfig(serverProperties);
        if (commandBigKeyMonitorConfig != null) {
            BigKeyMonitor.init(monitorIntervalSeconds);
            bigKeyHunter = new BigKeyHunter(commandBigKeyMonitorConfig);
        }
        ConverterConfig converterConfig = ConfigInitUtil.initConverterConfig(serverProperties);
        Converters converters = null;
        if (converterConfig != null) {
            converters = new Converters(converterConfig);
        }
        AuthCommandProcessor authCommandProcessor = new AuthCommandProcessor(ConfigInitUtil.initClientAuthProvider(serverProperties));
        this.commandInvokeConfig = new CommandInvokeConfig(authCommandProcessor, commandInterceptor, commandSpendTimeConfig,
                hotKeyCacheManager, hotKeyHunterManager, bigKeyHunter, converters);
    }

    private static final FastThreadLocal<CommandsTransponder> threadLocal = new FastThreadLocal<>();

    @Override
    public void invoke(ChannelHandlerContext ctx, ChannelInfo channelInfo, List<Command> commands) {
        if (commands.isEmpty()) return;
        try {
            CommandsTransponder trandponder = threadLocal.get();
            if (trandponder == null) {
                trandponder = new CommandsTransponder(chooser, commandInvokeConfig);
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
