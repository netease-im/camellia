package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
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

    private static final String CONF_KEY = "upstream.redis.hbase.command.execute.concurrent.enable";

    private final Map<String, Method> methodMap = new HashMap<>();
    private final RedisHBaseCommandProcessor processor;
    private final CamelliaHashedExecutor executor;

    private boolean concurrentEnable;

    public UpstreamRedisHBaseMixClientTemplate(CamelliaRedisTemplate redisTemplate, CamelliaHBaseTemplate hBaseTemplate) {
        Class<? extends IRedisHBaseCommandProcessor> clazz = IRedisHBaseCommandProcessor.class;
        CommandMethodUtil.initCommandFinderMethods(clazz, methodMap);
        processor = new RedisHBaseCommandProcessor(redisTemplate, hBaseTemplate);
        int poolSize = RedisHBaseConfiguration.executorPoolSize();
        executor = new CamelliaHashedExecutor("camellia-redis-hbase-mix-proxy", poolSize,
                RedisHBaseConfiguration::executorQueueSize, new CamelliaHashedExecutor.AbortPolicy());
        logger.info("UpstreamRedisHBaseMixClientTemplate init success");
        this.concurrentEnable = ProxyDynamicConf.getBoolean(CONF_KEY, false);
        ProxyDynamicConf.registerCallback(this::reloadConf);
    }

    @Override
    public List<CompletableFuture<Reply>> sendCommand(int db, List<Command> commands) {
        if (commands.isEmpty()) return new ArrayList<>();
        String consid = commands.get(0).getChannelInfo().getConsid();
        boolean concurrentEnable = concurrentEnable(commands);
        List<CompletableFuture<Reply>> futureList = new ArrayList<>(commands.size());
        for (Command command : commands) {
            CompletableFuture<Reply> future = new CompletableFuture<>();
            try {
                String hashKey = consid;
                if (concurrentEnable) {
                    List<byte[]> keys = command.getKeys();
                    if (keys == null || keys.isEmpty()) {
                        hashKey = consid;
                    } else {
                        hashKey = Utils.bytesToString(keys.get(0));
                    }
                }
                Task task = new Task(command, future);
                executor.submit(hashKey, () -> execute(task));
            } catch (Exception e) {
                ErrorLogCollector.collect(UpstreamRedisHBaseMixClientTemplate.class, "task submit fail, return TOO_BUSY, command = " + command.getName());
                future.complete(ErrorReply.TOO_BUSY);
            }
            futureList.add(future);
        }
        return futureList;
    }

    //是否并发执行命令，可以提升客户端pipeline提交命令时的执行效率，默认false
    //默认false的情况下，来自相同连接的命令顺序执行；如果设置为true，则来自相同连接的请求，会根据command关联的key进行哈希从而并发执行
    //备注：请确认好客户端的请求模型，如果是jedis这种阻塞模式可以开启，如果是lettuce且使用了异步模式则不能开启（会导致命令乱序执行）
    private boolean concurrentEnable(List<Command> commands) {
        if (!concurrentEnable) return false;
        for (Command command : commands) {
            List<byte[]> keys = command.getKeys();
            if (keys.size() > 1) {
                return false;
            }
        }
        return true;
    }

    private void reloadConf() {
        boolean concurrentEnable = ProxyDynamicConf.getBoolean(CONF_KEY, false);
        if ((this.concurrentEnable && !concurrentEnable) || (!this.concurrentEnable && concurrentEnable)) {
            logger.info("{} update, {} -> {}", CONF_KEY, this.concurrentEnable, concurrentEnable);
            this.concurrentEnable = concurrentEnable;
        }
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
