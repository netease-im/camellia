package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.base.resource.RedisType;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.conf.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/4/24
 */
public class KvCommandInvoker {
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1024), new CamelliaThreadFactory("camellia-kv-command"), new ThreadPoolExecutor.AbortPolicy());

    public static CompletableFuture<Reply> invoke(Command command) {
        RedisCommand redisCommand = command.getRedisCommand();
        byte[][] objects = command.getObjects();
        if (objects.length != 4) {
            return CompletableFuture.completedFuture(ErrorReply.argNumWrong(redisCommand));
        }
        CompletableFuture<Reply> future = new CompletableFuture<>();
        try {
            executor.submit(() -> {
                try {
                    String namespace = Utils.bytesToString(objects[2]);
                    String url = RedisType.RedisKV.getPrefix() + namespace;
                    IUpstreamClient client = GlobalRedisProxyEnv.getClientTemplateFactory().getEnv().getClientFactory().get(url);
                    client.sendCommand(-1, Collections.singletonList(command), Collections.singletonList(future));
                } catch (Exception e) {
                    ErrorLogCollector.collect(KvCommandInvoker.class, "invoke kv command error", e);
                    future.complete(ErrorReply.INTERNAL_ERROR);
                }
            });
        } catch (Exception e) {
            ErrorLogCollector.collect(KvCommandInvoker.class, "submit kv command task error", e);
            future.complete(ErrorReply.TOO_BUSY);
        }
        return future;
    }
}
