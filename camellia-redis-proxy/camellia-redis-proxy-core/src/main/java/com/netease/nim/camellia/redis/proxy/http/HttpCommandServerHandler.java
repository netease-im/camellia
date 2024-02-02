package com.netease.nim.camellia.redis.proxy.http;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.base.proxy.ProxyUtil;
import com.netease.nim.camellia.redis.proxy.auth.AuthCommandProcessor;
import com.netease.nim.camellia.redis.proxy.auth.ClientCommandUtil;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.ICommandInvoker;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by caojiajun on 2024/1/19
 */
@ChannelHandler.Sharable
public class HttpCommandServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LoggerFactory.getLogger(HttpCommandServerHandler.class);

    private static final DefaultFullHttpResponse HTTP_METHOD_NOT_ALLOWED = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED);
    private static final DefaultFullHttpResponse HTTP_NOT_FOUND = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
    private static final DefaultFullHttpResponse HTTP_BAD_REQUEST = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
    private static final DefaultFullHttpResponse HTTP_INTERNAL_SERVER_ERROR = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);

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
        notSupportedCommands.add(RedisCommand.CLIENT);
        notSupportedCommands.add(RedisCommand.READONLY);
    }

    private final ICommandInvoker invoker;

    public HttpCommandServerHandler(ICommandInvoker invoker) {
        super();
        this.invoker = invoker;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        ChannelInfo channelInfo = ChannelInfo.get(ctx);
        HttpCommandTaskQueue commandTaskQueue = channelInfo.getHttpCommandTaskQueue();
        HttpCommandTaskRequest request = new HttpCommandTaskRequest();
        request.setKeepAlive(isKeepAlive(httpRequest));
        request.setHttpVersion(httpRequest.protocolVersion());
        HttpCommandTask commandTask = new HttpCommandTask(commandTaskQueue, request);
        try {
            boolean add = commandTaskQueue.add(commandTask);
            if (!add) {
                ctx.close();
                logger.warn("HttpCommandTaskQueue full, client connect will be disconnect, remote.ip = {}", ctx.channel().remoteAddress());
                return;
            }
            HttpMethod method = httpRequest.method();
            if (method != HttpMethod.POST) {
                commandTask.write(HTTP_METHOD_NOT_ALLOWED);
                return;
            }
            String uri = httpRequest.uri();
            if (!uri.equals("/commands")) {
                commandTask.write(HTTP_NOT_FOUND);
                return;
            }
            //request
            ByteBuf content = httpRequest.content();
            byte[] data = new byte[content.readableBytes()];
            content.readBytes(data);
            HttpCommandRequest httpCommandRequest;
            try {
                httpCommandRequest = JSONObject.parseObject(new String(data, StandardCharsets.UTF_8), HttpCommandRequest.class);
            } catch (Exception e) {
                commandTask.write(HTTP_BAD_REQUEST);
                return;
            }
            request.setHttpCommandRequest(httpCommandRequest);

            List<Command> commands;
            try {
                commands = HttpCommandConverter.convert(httpCommandRequest);
            } catch (Exception e) {
                commandTask.write(HTTP_BAD_REQUEST);
                return;
            }
            //check supported command
            for (Command command : commands) {
                RedisCommand redisCommand = command.getRedisCommand();
                if (redisCommand == null || redisCommand.getSupportType() == RedisCommand.CommandSupportType.NOT_SUPPORT || notSupportedCommands.contains(redisCommand)) {
                    commandTask.write(wrapperError(httpCommandRequest, BAD_REQUEST, "command ‘" + command.getName() + "’ not support"));
                    return;
                }
                if (redisCommand.getCommandType() == RedisCommand.CommandType.PUB_SUB
                        && (redisCommand != RedisCommand.PUBLISH && redisCommand != RedisCommand.SPUBLISH && redisCommand != RedisCommand.PUBSUB)) {
                    commandTask.write(wrapperError(httpCommandRequest, BAD_REQUEST, "pub-sub commands not support"));
                    return;
                }
                command.setHttpCommandTask(commandTask);
            }
            //check auth
            AuthCommandProcessor authCommandProcessor = invoker.getCommandInvokeConfig().getAuthCommandProcessor();
            if (authCommandProcessor.isPasswordRequired() && httpCommandRequest.getPassword() == null) {
                commandTask.write(wrapperError(httpCommandRequest, FORBIDDEN, "password required"));
                return;
            }
            boolean pass = authCommandProcessor.checkPassword(channelInfo, httpCommandRequest.getUserName(), httpCommandRequest.getPassword());
            if (!pass) {
                commandTask.write(wrapperError(httpCommandRequest, FORBIDDEN, "check password fail"));
                return;
            }
            //check bid/bgroup
            if (httpCommandRequest.getBid() != null && httpCommandRequest.getBid() > 0 && httpCommandRequest.getBgroup() != null) {
                boolean updateClientName = ClientCommandUtil.updateClientName(channelInfo, ProxyUtil.buildClientName(httpCommandRequest.getBid(), httpCommandRequest.getBgroup()));
                if (!updateClientName) {
                    commandTask.write(wrapperError(httpCommandRequest, FORBIDDEN, "bid/bgroup conflict to username/password"));
                    return;
                }
            }
            //check db
            if (httpCommandRequest.getDb() > 0) {
                channelInfo.setDb(httpCommandRequest.getDb());
            }
            //reply
            invoker.invoke(ctx, channelInfo, commands);
        } catch (Exception e) {
            commandTask.write(HTTP_INTERNAL_SERVER_ERROR);
            ErrorLogCollector.collect(HttpCommandServerHandler.class, "internal error", e);
        }
    }

    private HttpCommandReply wrapperError(HttpCommandRequest request, int code, String msg) {
        HttpCommandReply reply = new HttpCommandReply();
        reply.setRequestId(request.getRequestId());
        reply.setCode(code);
        reply.setMsg(msg);
        return reply;
    }

    private boolean isKeepAlive(HttpRequest httpRequest) {
        boolean keepAlive = HttpUtil.isKeepAlive(httpRequest);
        if (keepAlive) {
            return httpRequest.protocolVersion().isKeepAliveDefault();
        } else {
            return false;
        }
    }
}
