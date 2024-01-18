package com.netease.nim.camellia.redis.proxy.http;

import com.netease.nim.camellia.redis.proxy.auth.ClientAuthProvider;
import com.netease.nim.camellia.redis.proxy.auth.ClientIdentity;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandInvokeConfig;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.monitor.CommandFailMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.plugin.rewrite.RouteRewriteResult;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.upstream.utils.CompletableFutureUtils;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

import java.util.*;
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
        notSupportedCommands.add(RedisCommand.CLIENT);
    }

    private final ClientAuthProvider clientAuthProvider;
    private final IUpstreamClientTemplateFactory factory;
    private final ProxyPluginFactory pluginFactory;
    private boolean eventLoopSetSuccess = false;
    private ProxyPluginInitResp proxyPluginInitResp;

    public HttpCommandTransponder(IUpstreamClientTemplateFactory factory, CommandInvokeConfig config) {
        this.clientAuthProvider = config.getAuthCommandProcessor().getClientAuthProvider();
        this.pluginFactory = config.getProxyPluginFactory();
        this.factory = factory;
        this.proxyPluginInitResp = pluginFactory.initPlugins();
        pluginFactory.registerPluginUpdate(() -> proxyPluginInitResp = pluginFactory.initPlugins());
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
            if (redisCommand.getCommandType() == RedisCommand.CommandType.PUB_SUB
                    && (redisCommand != RedisCommand.PUBLISH && redisCommand != RedisCommand.SPUBLISH && redisCommand != RedisCommand.PUBSUB)) {
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
            templateFuture = selectUpstreamClientTemplate(channelInfo, request);
        } catch (Exception e) {
            future.complete(wrapperError(request, FORBIDDEN, e.getMessage()));
            return future;
        }
        if (templateFuture == null) {
            future.complete(wrapperError(request, BAD_REQUEST, "bad request"));
            return future;
        }
        List<CompletableFuture<Reply>> futures = new ArrayList<>();

        List<Command> commandList = null;

        //plugins
        List<ProxyPlugin> requestPlugins = proxyPluginInitResp.getRequestPlugins();
        if (!requestPlugins.isEmpty()) {
            commandList = new ArrayList<>(commands.size());
            for (Command command : commands) {
                RouteRewriteResult rewriteResult = null;
                boolean pluginBreak = false;
                ProxyRequest proxyRequest = new ProxyRequest(request.getDb(), command, factory);
                for (ProxyPlugin plugin : requestPlugins) {
                    try {
                        ProxyPluginResponse pluginResponse = plugin.executeRequest(proxyRequest);
                        if (pluginResponse.getRouteRewriterResult() != null) {
                            rewriteResult = pluginResponse.getRouteRewriterResult();
                        }
                        if (!pluginResponse.isPass()) {
                            if (!commandList.isEmpty()) {
                                List<CompletableFuture<Reply>> flushFutures = flush(templateFuture, request.getDb(), commandList, proxyPluginInitResp);
                                futures.addAll(flushFutures);
                                commandList = new ArrayList<>();
                            }
                            CompletableFuture<Reply> pluginFuture = new CompletableFutureWithPlugins(proxyPluginInitResp, command, true);
                            pluginFuture.complete(pluginResponse.getReply());
                            futures.add(pluginFuture);
                            pluginBreak = true;
                            break;
                        }
                    } catch (Exception e) {
                        ErrorLogCollector.collect(HttpCommandTransponder.class, "executeRequest error", e);
                    }
                }
                if (pluginBreak) {
                    continue;
                }
                if (rewriteResult != null) {
                    long bid = rewriteResult.getBid();
                    String bgroup = rewriteResult.getBgroup();
                    if (!Objects.equals(bid, channelInfo.getBid()) || !bgroup.equals(channelInfo.getBgroup())) {
                        if (!commandList.isEmpty()) {
                            List<CompletableFuture<Reply>> flushFutures = flush(templateFuture, request.getDb(), commandList, proxyPluginInitResp);
                            futures.addAll(flushFutures);
                            commandList = new ArrayList<>();
                        }
                        List<CompletableFuture<Reply>> flush = flush(factory.getOrInitializeAsync(bid, bgroup), request.getDb(), Collections.singletonList(command), proxyPluginInitResp);
                        futures.addAll(flush);
                    }
                    continue;
                }
                if (command.getRedisCommand() == RedisCommand.INFO) {
                    if (!commandList.isEmpty()) {
                        List<CompletableFuture<Reply>> flushFutures = flush(templateFuture, request.getDb(), commandList, proxyPluginInitResp);
                        futures.addAll(flushFutures);
                        commandList = new ArrayList<>();
                    }
                    CompletableFuture<Reply> infoReply = ProxyInfoUtils.getInfoReply(command, factory);
                    CompletableFutureWithPlugins futureWithPlugins = new CompletableFutureWithPlugins(proxyPluginInitResp, command, false);
                    infoReply.thenAccept(futureWithPlugins::complete);
                    futures.add(futureWithPlugins);
                    continue;
                }
                commandList.add(command);
            }
        }
        //flush
        if (commandList == null) {
            futures = flush(templateFuture, request.getDb(), commands, proxyPluginInitResp);
        } else {
            if (!commandList.isEmpty()) {
                List<CompletableFuture<Reply>> flushFutures = flush(templateFuture, request.getDb(), commandList, proxyPluginInitResp);
                futures.addAll(flushFutures);
            }
        }
        //reply merge
        CompletableFuture<List<Reply>> replyFuture = CompletableFutureUtils.allOf(futures);
        replyFuture.thenAccept(list -> {
            List<Object> replies = HttpCommandReplyConverter.convert(list, request.isReplyBase64());
            HttpCommandReply reply = new HttpCommandReply();
            reply.setRequestId(request.getRequestId());
            reply.setCode(SUCCESS);
            reply.setCommands(request.getCommands());
            reply.setReplies(replies);
            future.complete(reply);
        });
        return future;
    }

    private List<CompletableFuture<Reply>> flush(CompletableFuture<IUpstreamClientTemplate> templateFuture, int db, List<Command> commands, ProxyPluginInitResp pluginInitResp) {
        List<CompletableFuture<Reply>> futures = new ArrayList<>(commands.size());
        for (Command command : commands) {
            futures.add(new CompletableFutureWithPlugins(pluginInitResp, command, false));
        }
        templateFuture.thenAccept(template -> {
            if (template == null) {
                for (int i = 0; i < commands.size(); i++) {
                    CompletableFuture<Reply> future = futures.get(i);
                    future.complete(ErrorReply.UPSTREAM_NOT_AVAILABLE);
                }
            } else {
                List<CompletableFuture<Reply>> list = template.sendCommand(db, commands);
                for (int i = 0; i < commands.size(); i++) {
                    CompletableFuture<Reply> future = futures.get(i);
                    list.get(i).thenAccept(future::complete);
                }
            }
        });
        return futures;
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

    private CompletableFuture<IUpstreamClientTemplate> selectUpstreamClientTemplate(ChannelInfo channelInfo, HttpCommandRequest request) {
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            if (!clientAuthProvider.isPasswordRequired()) {
                throw new IllegalArgumentException("no password is set");
            }
            channelInfo.setBid(request.getBid());
            channelInfo.setBgroup(request.getBgroup());
            return factory.getOrInitializeAsync(request.getBid(), request.getBgroup());
        } else {
            if (clientAuthProvider.isPasswordRequired()) {
                throw new IllegalArgumentException("password required");
            }
            ClientIdentity identity = clientAuthProvider.auth(request.getUserName(), request.getPassword());
            if (!identity.isPass()) {
                throw new IllegalArgumentException("check password fail");
            }
            if (identity.getBid() != null && identity.getBid() > 0 && identity.getBgroup() != null) {
                if (request.getBid() != null && !request.getBid().equals(identity.getBid())) {
                    throw new IllegalArgumentException("bid not match to password");
                }
                if (request.getBgroup() != null && !request.getBgroup().equals(identity.getBgroup())) {
                    throw new IllegalArgumentException("bgroup not match to password");
                }
                channelInfo.setBid(identity.getBid());
                channelInfo.setBgroup(identity.getBgroup());
                return factory.getOrInitializeAsync(identity.getBid(), identity.getBgroup());
            } else {
                channelInfo.setBid(request.getBid());
                channelInfo.setBgroup(request.getBgroup());
                return factory.getOrInitializeAsync(request.getBid(), request.getBgroup());
            }
        }
    }


}
