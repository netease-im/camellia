package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.*;
import com.netease.nim.camellia.redis.proxy.command.async.bigkey.CommandBigKeyMonitorConfig;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.CommandHotKeyMonitorConfig;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.CommandHotKeyCacheConfig;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEventHandler;
import com.netease.nim.camellia.redis.proxy.command.async.queue.disruptor.DisruptorCommandsEventHandler;
import com.netease.nim.camellia.redis.proxy.command.async.queue.lbq.LbqCommandsEventHandler;
import com.netease.nim.camellia.redis.proxy.command.async.queue.none.NoneQueueCommandsEventHandler;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.CommandSpendTimeConfig;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.conf.QueueType;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.util.ConfigInitUtil;
import io.netty.channel.ChannelFutureListener;
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
    private final QueueType queueType;
    private final CamelliaTranspondProperties transpondProperties;
    private final CommandInvokeConfig commandInvokeConfig;

    public AsyncCommandInvoker(CamelliaServerProperties serverProperties, CamelliaTranspondProperties transpondProperties) {
        this.transpondProperties = transpondProperties;
        this.chooser = new AsyncCamelliaRedisTemplateChooser(transpondProperties);
        this.queueType = transpondProperties.getRedisConf().getQueueType();
        if (queueType == QueueType.Disruptor) {
            ConfigInitUtil.checkDisruptorWaitStrategyClassName(transpondProperties);
        }

        CamelliaTranspondProperties.RedisConfProperties redisConf = transpondProperties.getRedisConf();
        int commandPipelineFlushThreshold = redisConf.getCommandPipelineFlushThreshold();

        CommandInterceptor commandInterceptor = ConfigInitUtil.initCommandInterceptor(serverProperties);
        CommandHotKeyMonitorConfig commandHotKeyMonitorConfig = ConfigInitUtil.initCommandHotKeyMonitorConfig(serverProperties);
        CommandSpendTimeConfig commandSpendTimeConfig = ConfigInitUtil.initCommandSpendTimeConfig(serverProperties);
        CommandHotKeyCacheConfig commandHotKeyCacheConfig = ConfigInitUtil.initHotKeyCacheConfig(serverProperties);
        CommandBigKeyMonitorConfig commandBigKeyMonitorConfig = ConfigInitUtil.initBigKeyMonitorConfig(serverProperties);

        this.commandInvokeConfig = new CommandInvokeConfig(commandPipelineFlushThreshold,
                commandInterceptor, commandSpendTimeConfig, commandHotKeyMonitorConfig, commandHotKeyCacheConfig, commandBigKeyMonitorConfig);
    }

    private static final FastThreadLocal<CommandsEventHandler> threadLocal = new FastThreadLocal<>();

    @Override
    public void invoke(ChannelHandlerContext ctx, ChannelInfo channelInfo, List<Command> commands) {
        if (commands.isEmpty()) return;
        try {
            CommandsEventHandler handler = threadLocal.get();
            if (handler == null) {
                if (queueType == QueueType.LinkedBlockingQueue) {
                    handler = new LbqCommandsEventHandler(chooser, commandInvokeConfig);
                    logger.info("LbqCommandsEventHandler init success");
                } else if (queueType == QueueType.Disruptor) {
                    CamelliaTranspondProperties.RedisConfProperties.DisruptorConf disruptorConf = transpondProperties.getRedisConf().getDisruptorConf();
                    handler = new DisruptorCommandsEventHandler(disruptorConf, chooser, commandInvokeConfig);
                    logger.info("DisruptorCommandsEventHandler init success");
                } else if (queueType == QueueType.None) {
                    handler = new NoneQueueCommandsEventHandler(chooser, commandInvokeConfig);
                    logger.info("NoneQueueCommandsEventHandler init success");
                } else {
                    throw new CamelliaRedisException("unknown queueType");
                }
                threadLocal.set(handler);
            }
            boolean success = handler.getProducer().publishEvent(channelInfo, commands);
            if (!success) {
                logger.error("CommandsEventProducer publishEvent fail");
                ctx.writeAndFlush(ErrorReply.TOO_BUSY).addListener((ChannelFutureListener) future -> ctx.close());
            }
        } catch (Exception e) {
            ctx.close();
            logger.error(e.getMessage(), e);
        }
    }
}
