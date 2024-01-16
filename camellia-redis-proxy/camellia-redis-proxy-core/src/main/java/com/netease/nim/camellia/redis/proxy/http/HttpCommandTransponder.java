package com.netease.nim.camellia.redis.proxy.http;

import com.netease.nim.camellia.redis.proxy.auth.ClientAuthProvider;
import com.netease.nim.camellia.redis.proxy.auth.ClientIdentity;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.upstream.utils.CompletableFutureUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/1/16
 */
public class HttpCommandTransponder {

    private static final int SUCCESS = 200;
    private static final int BAD_REQUEST = 400;
    private static final int FORBIDDEN = 403;

    private static final Set<RedisCommand> notSupportedCommands = new HashSet<>();
    static {
        notSupportedCommands.add(RedisCommand.PROXY);
        notSupportedCommands.add(RedisCommand.SELECT);
        notSupportedCommands.add(RedisCommand.HELLO);
        notSupportedCommands.add(RedisCommand.AUTH);
        notSupportedCommands.add(RedisCommand.SENTINEL);
        notSupportedCommands.add(RedisCommand.CLUSTER);
        notSupportedCommands.add(RedisCommand.ASKING);
        notSupportedCommands.add(RedisCommand.QUIT);
        notSupportedCommands.add(RedisCommand.INFO);
    }

    private final ClientAuthProvider clientAuthProvider;
    private final IUpstreamClientTemplateFactory factory;
    private boolean eventLoopSetSuccess = false;

    public HttpCommandTransponder(ClientAuthProvider clientAuthProvider, IUpstreamClientTemplateFactory factory) {
        this.clientAuthProvider = clientAuthProvider;
        this.factory = factory;
    }

    public CompletableFuture<HttpCommandReply> transpond(ChannelInfo channelInfo, HttpCommandRequest request) {
        if (!eventLoopSetSuccess) {
            if (channelInfo.getCtx() != null) {
                RedisConnectionHub.getInstance().updateEventLoop(channelInfo.getCtx().channel().eventLoop());
                eventLoopSetSuccess = true;
            }
        }
        //reply
        CompletableFuture<HttpCommandReply> future = new CompletableFuture<>();
        //convert to commands
        List<Command> commands;
        try {
            commands = HttpCommandConverter.convert(request);
        } catch (Exception e) {
            future.complete(wrapperError(request, BAD_REQUEST, "command convert error"));
            return future;
        }
        //check supported command
        for (Command command : commands) {
            RedisCommand redisCommand = command.getRedisCommand();
            if (redisCommand == null || redisCommand.getSupportType() == RedisCommand.CommandSupportType.NOT_SUPPORT || notSupportedCommands.contains(redisCommand)) {
                future.complete(wrapperError(request, BAD_REQUEST, "command ‘" + command.getName() + "’ not support"));
                return future;
            }
            if (command.isBlocking()) {
                future.complete(wrapperError(request, BAD_REQUEST, "blocking commands not support"));
                return future;
            }
            if (redisCommand.getCommandType() == RedisCommand.CommandType.PUB_SUB) {
                future.complete(wrapperError(request, BAD_REQUEST, "pub-sub commands not support"));
                return future;
            }
            if (redisCommand.getCommandType() == RedisCommand.CommandType.TRANSACTION) {
                future.complete(wrapperError(request, BAD_REQUEST, "transaction commands not support"));
                return future;
            }
            command.setChannelInfo(channelInfo);
        }
        //select upstream client template
        CompletableFuture<IUpstreamClientTemplate> templateFuture;
        try {
            templateFuture = selectUpstreamClientTemplate(request);
        } catch (Exception e) {
            future.complete(wrapperError(request, FORBIDDEN, e.getMessage()));
            return future;
        }
        if (templateFuture == null) {
            future.complete(wrapperError(request, BAD_REQUEST, "bad request"));
            return future;
        }
        templateFuture.thenAccept(template -> {
            //send commands
            List<CompletableFuture<Reply>> futures = template.sendCommand(request.getDb(), commands);
            //send replies
            CompletableFuture<List<Reply>> replyFuture = CompletableFutureUtils.allOf(futures);
            replyFuture.thenAccept(list -> {
                List<Object> replies = HttpCommandReplyConverter.convert(list);
                HttpCommandReply reply = new HttpCommandReply();
                reply.setRequestId(request.getRequestId());
                reply.setCode(SUCCESS);
                reply.setCommands(request.getCommands());
                reply.setReplies(replies);
                future.complete(reply);
            });
        });
        return future;
    }

    private HttpCommandReply wrapperError(HttpCommandRequest request, int code, String msg) {
        HttpCommandReply reply = new HttpCommandReply();
        reply.setRequestId(request.getRequestId());
        reply.setCode(code);
        reply.setMsg(msg);
        return reply;
    }

    private CompletableFuture<IUpstreamClientTemplate> selectUpstreamClientTemplate(HttpCommandRequest request) {
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            if (!clientAuthProvider.isPasswordRequired()) {
                throw new IllegalArgumentException("no password is set");
            }
            return factory.getOrInitializeAsync(null, null);
        } else {
            if (clientAuthProvider.isPasswordRequired()) {
                throw new IllegalArgumentException("missing password");
            }
            ClientIdentity identity = clientAuthProvider.auth(request.getUserName(), request.getPassword());
            if (!identity.isPass()) {
                throw new IllegalArgumentException("wrong password");
            }
            return factory.getOrInitializeAsync(identity.getBid(), identity.getBgroup());
        }
    }


}
