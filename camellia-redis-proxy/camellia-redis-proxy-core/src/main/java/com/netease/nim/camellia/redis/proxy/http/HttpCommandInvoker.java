package com.netease.nim.camellia.redis.proxy.http;

import com.netease.nim.camellia.redis.proxy.auth.ClientAuthProvider;
import com.netease.nim.camellia.redis.proxy.command.ICommandInvoker;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import io.netty.util.concurrent.FastThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/1/16
 */
public class HttpCommandInvoker {

    private static final Logger logger = LoggerFactory.getLogger(HttpCommandInvoker.class);
    private final ICommandInvoker invoker;

    public HttpCommandInvoker(ICommandInvoker invoker) {
        this.invoker = invoker;
    }

    private static final FastThreadLocal<HttpCommandTransponder> threadLocal = new FastThreadLocal<>();

    public CompletableFuture<HttpCommandReply> invoke(ChannelInfo channelInfo, HttpCommandRequest request) {
        HttpCommandTransponder transponder = threadLocal.get();
        if (transponder == null) {
            IUpstreamClientTemplateFactory factory = invoker.getUpstreamClientTemplateFactory();
            ClientAuthProvider clientAuthProvider = invoker.getCommandInvokeConfig().getAuthCommandProcessor().getClientAuthProvider();
            transponder = new HttpCommandTransponder(clientAuthProvider, factory);
            logger.info("HttpCommandTransponder init success");
            threadLocal.set(transponder);
        }
        return transponder.transpond(channelInfo, request);
    }
}
