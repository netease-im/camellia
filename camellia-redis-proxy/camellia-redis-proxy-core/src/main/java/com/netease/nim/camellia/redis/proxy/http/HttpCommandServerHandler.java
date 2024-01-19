package com.netease.nim.camellia.redis.proxy.http;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.base.proxy.ProxyUtil;
import com.netease.nim.camellia.redis.proxy.auth.AuthCommandProcessor;
import com.netease.nim.camellia.redis.proxy.auth.ClientCommandUtil;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.ICommandInvoker;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.CommandFailMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by caojiajun on 2024/1/19
 */
@ChannelHandler.Sharable
public class HttpCommandServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final DefaultFullHttpResponse HTTP_METHOD_NOT_ALLOWED = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED);
    private static final DefaultFullHttpResponse HTTP_NOT_FOUND = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
    private static final DefaultFullHttpResponse HTTP_BAD_REQUEST = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
    private static final DefaultFullHttpResponse HTTP_INTERNAL_SERVER_ERROR = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);

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
        notSupportedCommands.add(RedisCommand.CLIENT);
    }

    private final ICommandInvoker invoker;

    public HttpCommandServerHandler(ICommandInvoker invoker) {
        super();
        this.invoker = invoker;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        ChannelInfo channelInfo = ChannelInfo.get(ctx);
        HttpCommandAggregator aggregator = channelInfo.getHttpCommandAggregator();
        try {
            HttpMethod method = httpRequest.method();
            if (method != HttpMethod.POST) {
                boolean success = aggregator.addRequest(httpRequest, null);
                if (!success) {
                    ctx.close();
                    aggregator.failMonitor("HttpRequestBusy");
                    return;
                }
                aggregator.addResponse(HTTP_METHOD_NOT_ALLOWED);
                return;
            }
            String uri = httpRequest.uri();
            if (!uri.equals("/commands")) {
                boolean success = aggregator.addRequest(httpRequest, null);
                if (!success) {
                    ctx.close();
                    aggregator.failMonitor("HttpRequestBusy");
                    return;
                }
                aggregator.addResponse(HTTP_NOT_FOUND);
                return;
            }
            //request
            ByteBuf content = httpRequest.content();
            byte[] data = new byte[content.readableBytes()];
            content.readBytes(data);
            HttpCommandRequest request;
            try {
                request = JSONObject.parseObject(new String(data, StandardCharsets.UTF_8), HttpCommandRequest.class);
            } catch (Exception e) {
                boolean success = aggregator.addRequest(httpRequest, null);
                if (!success) {
                    ctx.close();
                    aggregator.failMonitor("HttpRequestBusy");
                    return;
                }
                aggregator.addResponse(HTTP_BAD_REQUEST);
                return;
            }

            List<Command> commands;
            try {
                commands = HttpCommandConverter.convert(request);
            } catch (Exception e) {
                boolean success = aggregator.addRequest(httpRequest, null);
                if (!success) {
                    ctx.close();
                    aggregator.failMonitor("HttpRequestBusy");
                    return;
                }
                aggregator.addResponse(HTTP_BAD_REQUEST);
                return;
            }
            boolean success = aggregator.addRequest(httpRequest, request);
            if (!success) {
                ctx.close();
                aggregator.failMonitor("HttpRequestBusy");
                return;
            }
            //check supported command
            for (Command command : commands) {
                RedisCommand redisCommand = command.getRedisCommand();
                if (redisCommand == null || redisCommand.getSupportType() == RedisCommand.CommandSupportType.NOT_SUPPORT || notSupportedCommands.contains(redisCommand)) {
                    aggregator.addResponse(wrapperError(request, BAD_REQUEST, "command ‘" + command.getName() + "’ not support"));
                    return;
                }
                if (command.isBlocking()) {
                    aggregator.addResponse(wrapperError(request, BAD_REQUEST, "blocking commands not support"));
                    return;
                }
                if (redisCommand.getCommandType() == RedisCommand.CommandType.PUB_SUB
                        && (redisCommand != RedisCommand.PUBLISH && redisCommand != RedisCommand.SPUBLISH && redisCommand != RedisCommand.PUBSUB)) {
                    aggregator.addResponse(wrapperError(request, BAD_REQUEST, "pub-sub commands not support"));
                    return;
                }
                if (redisCommand.getCommandType() == RedisCommand.CommandType.TRANSACTION) {
                    aggregator.addResponse(wrapperError(request, BAD_REQUEST, "transaction commands not support"));
                    return;
                }
            }
            //check auth
            AuthCommandProcessor authCommandProcessor = invoker.getCommandInvokeConfig().getAuthCommandProcessor();
            if (authCommandProcessor.isPasswordRequired() && request.getPassword() == null) {
                aggregator.addResponse(wrapperError(request, FORBIDDEN, "password required"));
                return;
            }
            boolean pass = authCommandProcessor.checkPassword(channelInfo, request.getUserName(), request.getPassword());
            if (!pass) {
                aggregator.addResponse(wrapperError(request, FORBIDDEN, "check password fail"));
                return;
            }
            //check bid/bgroup
            if (request.getBid() != null && request.getBid() > 0 && request.getBgroup() != null) {
                boolean updateClientName = ClientCommandUtil.updateClientName(channelInfo, ProxyUtil.buildClientName(request.getBid(), request.getBgroup()));
                if (!updateClientName) {
                    aggregator.addResponse(wrapperError(request, FORBIDDEN, "bid/bgroup conflict to username/password"));
                    return;
                }
            }
            //check db
            if (request.getDb() > 0) {
                channelInfo.setDb(request.getDb());
            }
            //reply
            invoker.invoke(ctx, channelInfo, commands);
        } catch (Exception e) {
            boolean success = aggregator.addRequest(httpRequest, null);
            if (!success) {
                ctx.close();
                aggregator.failMonitor("HttpRequestBusy");
                return;
            }
            aggregator.addResponse(HTTP_INTERNAL_SERVER_ERROR);
        }
    }

    private HttpCommandReply wrapperError(HttpCommandRequest request, int code, String msg) {
        HttpCommandReply reply = new HttpCommandReply();
        reply.setRequestId(request.getRequestId());
        reply.setCode(code);
        reply.setMsg(msg);
        if (ProxyMonitorCollector.isMonitorEnable() && code != SUCCESS) {
            CommandFailMonitor.incr("code=" + code + ",msg=" + msg);
        }
        return reply;
    }
}
