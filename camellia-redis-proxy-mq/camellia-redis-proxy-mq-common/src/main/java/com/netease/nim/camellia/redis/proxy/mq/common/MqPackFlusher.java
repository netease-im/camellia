package com.netease.nim.camellia.redis.proxy.mq.common;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncClient;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncNettyClientFactory;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class MqPackFlusher {

    private static final Logger logger = LoggerFactory.getLogger(MqPackFlusher.class);

    /**
     * 发送命令给redis
     * @param command 命令
     * @param url redis地址
     */
    public static void flush(Command command, String url) {
        try {
            AsyncClient client = AsyncNettyClientFactory.DEFAULT.get(url);
            client.sendCommand(Collections.singletonList(command),
                    Collections.singletonList(new CompletableFuture<>()));
            if (logger.isDebugEnabled()) {
                logger.debug("flush command success, command = {}, url = {}, keys = {}", command.getName(), url, command.getKeysStr());
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(MqPackFlusher.class, "flush error", e);
        }
    }
}
