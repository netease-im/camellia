package com.netease.nim.camellia.redis.proxy.command.sync;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.util.CommandInvokerUtil;
import com.netease.nim.camellia.redis.proxy.util.CommandMethodUtil;
import com.netease.nim.camellia.redis.proxy.util.ErrorHandlerUtil;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/11/5.
 */
public class SyncCommandInvoker implements CommandInvoker {

    private static final Logger logger = LoggerFactory.getLogger(SyncCommandInvoker.class);

    private final Map<String, Method> methodMap = new HashMap<>();
    private final Map<String, Method> pipelineMethodMap = new HashMap<>();
    private final SyncCommandProcessorChooser chooser;

    public SyncCommandInvoker(CamelliaTranspondProperties transpondProperties) {
        this.chooser = new SyncCommandProcessorChooser(transpondProperties);
        init();
    }

    private void init() {
        Class<? extends ISyncCommandProcessor> clazz = ISyncCommandProcessor.class;
        CommandMethodUtil.initCommandFinderMethods(clazz, methodMap);
        Class<? extends ISyncCommandProcessorPipeline> pipelineClazz = ISyncCommandProcessorPipeline.class;
        CommandMethodUtil.initCommandFinderMethods(pipelineClazz, pipelineMethodMap);
    }

    @Override
    public void invoke(ChannelHandlerContext ctx, ChannelInfo channelInfo, List<Command> commands) {
        if (commands.isEmpty()) return;
        if (logger.isDebugEnabled()) {
            List<String> commandNameList = new ArrayList<>(commands.size());
            for (Command command : commands) {
                commandNameList.add(command.getName());
            }
            logger.debug("receive commands, commands.size = {}, consid = {}, commands = {}",
                    commands.size(), channelInfo.getConsid(), commandNameList);
        }

        SyncCommandProcessor processor = chooser.choose(channelInfo);
        if (processor == null) {
            for (int i=0; i<commands.size(); i++) {
                ctx.writeAndFlush(ErrorReply.NOT_AVAILABLE);
                debugLog(ErrorReply.NOT_AVAILABLE, channelInfo);
            }
            logger.warn("CommandProcessor not available, consid = {}", channelInfo.getConsid());
            return;
        }
        if (commands.size() == 1) {
            Command command = commands.get(0);
            try {
                Method method = methodMap.get(command.getName());
                if (method == null) {
                    logger.warn("command not support, command = {}, consid = {}", command.getName(), channelInfo.getConsid());
                    ctx.writeAndFlush(ErrorReply.NOT_SUPPORT);
                    debugLog(ErrorReply.NOT_SUPPORT, channelInfo);
                    return;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                Object[] parameters = new Object[parameterTypes.length];
                command.fillParameters(parameterTypes, parameters);

                Reply reply = (Reply) method.invoke(processor, parameters);
                ctx.writeAndFlush(reply);
                debugLog(reply, channelInfo);
            } catch (Throwable e) {
                Reply reply = handlerError(e, command.getName());
                ctx.writeAndFlush(reply);
                debugLog(reply, channelInfo);
            }
        } else {
            SyncCommandProcessorPipeline pipelined = processor.pipelined();
            try {
                List<PipelineResponse> list = new ArrayList<>(commands.size());
                for (Command command : commands) {
                    try {
                        Method pipelineMethod = pipelineMethodMap.get(command.getName());
                        if (pipelineMethod == null) {
                            handlerPipelineResponse(ctx, list, pipelined, channelInfo);
                            Method syncMethod = methodMap.get(command.getName());
                            if (syncMethod == null) {
                                logger.warn("CommandProcessor not available, consid = {}", channelInfo.getConsid());
                                ctx.writeAndFlush(ErrorReply.NOT_SUPPORT);
                                debugLog(ErrorReply.NOT_SUPPORT, channelInfo);
                            } else {
                                Reply reply = (Reply) CommandInvokerUtil.invoke(syncMethod, command, processor);
                                ctx.writeAndFlush(reply);
                                debugLog(reply, channelInfo);
                            }
                        } else {
                            try {
                                PipelineResponse response = (PipelineResponse) CommandInvokerUtil.invoke(pipelineMethod, command, pipelined);
                                list.add(response);
                            } catch (Exception e) {
                                Reply reply = handlerError(e, command.getName());
                                list.add(new PipelineResponse(reply));
                            }
                        }
                    } catch (Throwable e) {
                        handlerPipelineResponse(ctx, list, pipelined, channelInfo);
                        Reply reply = handlerError(e, command.getName());
                        ctx.writeAndFlush(reply);
                        debugLog(reply, channelInfo);
                    }
                }
                handlerPipelineResponse(ctx, list, pipelined, channelInfo);
            } finally {
                pipelined.close();
            }
        }
    }

    private void handlerPipelineResponse(ChannelHandlerContext ctx, List<PipelineResponse> list, SyncCommandProcessorPipeline pipelined, ChannelInfo channelInfo) {
        if (list.isEmpty()) return;
        try {
            pipelined.sync();
        } catch (Throwable e) {
            Reply reply = handlerError(e, "pipeline_sync");
            for (int i=0; i<list.size(); i++) {
                ctx.writeAndFlush(reply);
                debugLog(reply, channelInfo);
            }
            list.clear();
            return;
        }
        for (PipelineResponse response : list) {
            Reply reply;
            try {
                reply = response.get();
            } catch (Exception e) {
                reply = handlerError(e, "pipeline_get");
            }
            ctx.writeAndFlush(reply);
            debugLog(reply, channelInfo);
        }
        list.clear();
    }

    private Reply handlerError(Throwable e, String msg) {
        e = ErrorHandlerUtil.handler(e);
        String message = ErrorHandlerUtil.redisErrorMessage(e);
        String log = "invoke error, msg = " + msg + ",e=" + e.toString();
        ErrorLogCollector.collect(SyncCommandInvoker.class, log);
        return new ErrorReply(message);
    }

    private void debugLog(Reply reply, ChannelInfo channelInfo) {
        if (logger.isDebugEnabled()) {
            logger.debug("send reply = {}, consid = {}", reply.getClass().getSimpleName(), channelInfo.getConsid());
        }
    }
}
