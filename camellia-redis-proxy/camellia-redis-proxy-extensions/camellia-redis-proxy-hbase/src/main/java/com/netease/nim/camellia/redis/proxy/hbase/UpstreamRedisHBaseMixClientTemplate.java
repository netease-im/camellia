package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplate;
import com.netease.nim.camellia.redis.proxy.util.*;
import com.netease.nim.camellia.tools.executor.CamelliaHashedExecutor;
import com.netease.nim.camellia.tools.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/1/31
 */
public class UpstreamRedisHBaseMixClientTemplate implements IUpstreamClientTemplate {

    private static final Logger logger = LoggerFactory.getLogger(UpstreamRedisHBaseMixClientTemplate.class);

    private final Map<String, Method> methodMap = new HashMap<>();
    private final RedisHBaseCommandProcessor processor;
    private final CamelliaHashedExecutor executor;

    public UpstreamRedisHBaseMixClientTemplate(CamelliaRedisTemplate redisTemplate, CamelliaHBaseTemplate hBaseTemplate) {
        Class<? extends IRedisHBaseCommandProcessor> clazz = IRedisHBaseCommandProcessor.class;
        CommandMethodUtil.initCommandFinderMethods(clazz, methodMap);
        processor = new RedisHBaseCommandProcessor(redisTemplate, hBaseTemplate);
        int poolSize = RedisHBaseConfiguration.executorPoolSize();
        executor = new CamelliaHashedExecutor("camellia-redis-hbase-mix-proxy", poolSize,
                RedisHBaseConfiguration::executorQueueSize, new CamelliaHashedExecutor.AbortPolicy());
        logger.info("UpstreamRedisHBaseMixClientTemplate init success");
    }

    @Override
    public List<CompletableFuture<Reply>> sendCommand(List<Command> commands) {
        if (commands.isEmpty()) return new ArrayList<>();
        String consid = commands.get(0).getChannelInfo().getConsid();
        List<CompletableFuture<Reply>> futureList = new ArrayList<>(commands.size());
        for (Command command : commands) {
            CompletableFuture<Reply> future = new CompletableFuture<>();
            try {
                Task task = new Task(command, future);
                executor.submit(consid, () -> execute(task));
            } catch (Exception e) {
                ErrorLogCollector.collect(UpstreamRedisHBaseMixClientTemplate.class, "task submit fail, return TOO_BUSY, command = " + command.getName());
                future.complete(ErrorReply.TOO_BUSY);
            }
            futureList.add(future);
        }
        return futureList;
    }

    private static class Task {
        private final Command command;
        private final CompletableFuture<Reply> future;
        private final long createTime = TimeCache.currentMillis;

        public Task(Command command, CompletableFuture<Reply> future) {
            this.command = command;
            this.future = future;
        }
    }

    private void execute(Task task) {
        Command command = task.command;
        CompletableFuture<Reply> future = task.future;
        try {
            if (TimeCache.currentMillis - task.createTime > RedisHBaseConfiguration.taskExpireMillis()) {
                future.complete(ErrorReply.TOO_BUSY);
                ErrorLogCollector.collect(UpstreamRedisHBaseMixClientTemplate.class, "task expire, return TOO_BUSY, command = " + command.getName());
                return;
            }
            ChannelInfo channelInfo = command.getChannelInfo();
            Method method = methodMap.get(command.getName());
            Reply reply;
            if (method == null) {
                logger.warn("not support command = {}, return NOT_SUPPORT, consid = {}", command.getName(), channelInfo.getConsid());
                reply = ErrorReply.NOT_SUPPORT;
            } else {
                reply = (Reply) CommandInvokerUtil.invoke(method, command, processor);
            }
            future.complete(reply);
        } catch (Exception e) {
            future.complete(handlerError(e, command.getName()));
        }
    }

    private Reply handlerError(Throwable e, String msg) {
        e = ExceptionUtils.onError(e);
        String message = ErrorHandlerUtil.redisErrorMessage(e);
        String log = "invoke error, msg = " + msg + ",e=" + e;
        ErrorLogCollector.collect(UpstreamRedisHBaseMixClientTemplate.class, log);
        return new ErrorReply(message);
    }
}
