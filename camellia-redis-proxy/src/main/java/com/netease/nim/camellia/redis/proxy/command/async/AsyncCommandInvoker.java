package com.netease.nim.camellia.redis.proxy.command.async;

import com.lmax.disruptor.WaitStrategy;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.*;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEventHandler;
import com.netease.nim.camellia.redis.proxy.command.async.queue.disruptor.DisruptorCommandsEventHandler;
import com.netease.nim.camellia.redis.proxy.command.async.queue.lbq.LbqCommandsEventHandler;
import com.netease.nim.camellia.redis.proxy.command.async.queue.none.NoneQueueCommandsEventHandler;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.conf.QueueType;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
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
    private final boolean commandSpendTimeMonitorEnable;
    private final long slowCommandThresholdMillisTime;
    private final int commandPipelineFlushThreshold;
    private CommandInterceptor commandInterceptor = null;
    private final QueueType queueType;
    private final CamelliaTranspondProperties transpondProperties;

    public AsyncCommandInvoker(CamelliaServerProperties serverProperties, CamelliaTranspondProperties transpondProperties) {
        this.transpondProperties = transpondProperties;
        this.slowCommandThresholdMillisTime = serverProperties.getSlowCommandThresholdMillisTime();
        this.commandSpendTimeMonitorEnable = serverProperties.isMonitorEnable() && serverProperties.isCommandSpendTimeMonitorEnable();
        this.chooser = new AsyncCamelliaRedisTemplateChooser(transpondProperties);
        this.queueType = transpondProperties.getRedisConf().getQueueType();
        CamelliaTranspondProperties.RedisConfProperties redisConf = transpondProperties.getRedisConf();
        this.commandPipelineFlushThreshold = redisConf.getCommandPipelineFlushThreshold();
        String commandInterceptorClassName = serverProperties.getCommandInterceptorClassName();
        if (commandInterceptorClassName != null) {
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(commandInterceptorClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(commandInterceptorClassName);
                }
                commandInterceptor = (CommandInterceptor) clazz.newInstance();
                logger.info("CommandInterceptor init success, class = {}", commandInterceptorClassName);
            } catch (Exception e) {
                logger.error("CommandInterceptor init error, class = {}", commandInterceptorClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        if (queueType == QueueType.Disruptor) {
            CamelliaTranspondProperties.RedisConfProperties.DisruptorConf disruptorConf = transpondProperties.getRedisConf().getDisruptorConf();
            if (disruptorConf != null) {
                String waitStrategyClassName = disruptorConf.getWaitStrategyClassName();
                if (waitStrategyClassName != null) {
                    try {
                        Class<?> clazz = Class.forName(waitStrategyClassName);
                        Object o = clazz.newInstance();
                        if (!(o instanceof WaitStrategy)) {
                            throw new CamelliaRedisException("not instance of com.lmax.disruptor.WaitStrategy");
                        }
                    } catch (CamelliaRedisException e) {
                        throw e;
                    } catch (Exception e) {
                        logger.error("CommandInterceptor init error, class = {}", commandInterceptorClassName, e);
                        throw new CamelliaRedisException(e);
                    }
                }
            }
        }
    }

    private static final FastThreadLocal<CommandsEventHandler> threadLocal = new FastThreadLocal<>();

    @Override
    public void invoke(ChannelHandlerContext ctx, ChannelInfo channelInfo, List<Command> commands) {
        if (commands.isEmpty()) return;
        try {
            CommandsEventHandler handler = threadLocal.get();
            if (handler == null) {
                if (queueType == QueueType.LinkedBlockingQueue) {
                    handler = new LbqCommandsEventHandler(chooser, commandInterceptor,
                            commandPipelineFlushThreshold, commandSpendTimeMonitorEnable, slowCommandThresholdMillisTime);
                    logger.info("LbqCommandsEventHandler init success");
                } else if (queueType == QueueType.Disruptor) {
                    CamelliaTranspondProperties.RedisConfProperties.DisruptorConf disruptorConf = transpondProperties.getRedisConf().getDisruptorConf();
                    handler = new DisruptorCommandsEventHandler(disruptorConf, chooser, commandInterceptor,
                            commandPipelineFlushThreshold, commandSpendTimeMonitorEnable, slowCommandThresholdMillisTime);
                    logger.info("DisruptorCommandsEventHandler init success");
                } else if (queueType == QueueType.None) {
                    handler = new NoneQueueCommandsEventHandler(chooser, commandInterceptor,
                            commandPipelineFlushThreshold, commandSpendTimeMonitorEnable, slowCommandThresholdMillisTime);
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
