package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.auth.AuthCommandProcessor;
import com.netease.nim.camellia.redis.proxy.auth.ClientCommandUtil;
import com.netease.nim.camellia.redis.proxy.auth.ConnectLimiter;
import com.netease.nim.camellia.redis.proxy.auth.HelloCommandUtil;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.plugin.rewrite.RouteRewriteResult;
import com.netease.nim.camellia.redis.proxy.sentinel.ProxySentinelModeProcessor;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.ChannelMonitor;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2021/5/26
 */
public class CommandsTransponder {

    private static final Logger logger = LoggerFactory.getLogger(CommandsTransponder.class);

    private final AuthCommandProcessor authCommandProcessor;
    private final ProxyClusterModeProcessor clusterModeProcessor;
    private final ProxySentinelModeProcessor sentinelModeProcessor;
    private final IUpstreamClientTemplateFactory factory;
    private final ProxyPluginFactory proxyPluginFactory;
    private final ProxyCommandProcessor proxyCommandProcessor;
    private boolean eventLoopSetSuccess = false;

    private ProxyPluginInitResp proxyPluginInitResp;

    public CommandsTransponder(IUpstreamClientTemplateFactory factory, CommandInvokeConfig commandInvokeConfig) {
        this.factory = factory;
        this.authCommandProcessor = commandInvokeConfig.getAuthCommandProcessor();
        this.clusterModeProcessor = commandInvokeConfig.getClusterModeProcessor();
        this.sentinelModeProcessor = commandInvokeConfig.getSentinelModeProcessor();
        this.proxyPluginFactory = commandInvokeConfig.getProxyPluginFactory();
        this.proxyCommandProcessor = commandInvokeConfig.getProxyCommandProcessor();
        this.proxyPluginInitResp = proxyPluginFactory.initPlugins();
        // 刷新插件用的
        proxyPluginFactory.registerPluginUpdate(() -> proxyPluginInitResp = proxyPluginFactory.initPlugins());
    }

    public void transpond(ChannelInfo channelInfo, List<Command> commands) {
        if (!eventLoopSetSuccess) {
            if (channelInfo.getCtx() != null) {
                RedisConnectionHub.getInstance().updateEventLoop(channelInfo.getCtx().channel().eventLoop());
                eventLoopSetSuccess = true;
            }
        }
        try {
            boolean hasCommandsSkip = false;
            //  任务队列
            CommandTaskQueue taskQueue = channelInfo.getCommandTaskQueue();

            if (logger.isDebugEnabled()) {
                List<String> commandNameList = new ArrayList<>(commands.size());
                for (Command command : commands) {
                    commandNameList.add(command.getName());
                }
                logger.debug("receive commands, commands.size = {}, consid = {}, commands = {}",
                        commands.size(), taskQueue.getChannelInfo().getConsid(), commandNameList);
            }

            if (channelInfo.getChannelStats() == ChannelInfo.ChannelStats.INVALID) {
                channelInfo.getCtx().channel().close();
                logger.warn("too many connects, connect will be force closed, consid = {}, client.addr = {}",
                        channelInfo.getConsid(), channelInfo.getCtx().channel().remoteAddress());
                return;
            }

            List<CommandTask> tasks = new ArrayList<>(commands.size());
            ChannelHandlerContext ctx = channelInfo.getCtx();

            for (Command command : commands) {
                //设置channelInfo
                command.setChannelInfo(channelInfo);

                //任务
                CommandTask task = new CommandTask(taskQueue, command, proxyPluginInitResp.getReplyPlugins());
                boolean add = taskQueue.add(task);
                if (!add) {
                    taskQueue.clear();
                    logger.warn("CommandTaskQueue full, client connect will be disconnect, remote.ip = {}", ctx.channel().remoteAddress());
                    ctx.writeAndFlush(ErrorReply.TOO_BUSY).addListener((ChannelFutureListener) future -> ctx.close());
                    return;
                }

                RouteRewriteResult rewriterResult = null;
                List<ProxyPlugin> requestPlugins = proxyPluginInitResp.getRequestPlugins();
                if (!requestPlugins.isEmpty()) {
                    boolean pluginBreak = false;
                    //执行插件，跟spring boot的拦截器一样的思维，算是一种责任链，上一个没通过下一个plugin也不会执行了
                    ProxyRequest request = new ProxyRequest(channelInfo.getDb(), command, factory);
                    for (ProxyPlugin plugin : proxyPluginInitResp.getRequestPlugins()) {
                        try {
                            ProxyPluginResponse response = plugin.executeRequest(request);
                            RouteRewriteResult result = response.getRouteRewriterResult();
                            if (result != null) {
                                rewriterResult = result;
                            }
                            if (!response.isPass()) {
                                reply(channelInfo, task, command.getRedisCommand(), response.getReply(), true);
                                hasCommandsSkip = true;
                                pluginBreak = true;
                                break;
                            }
                        } catch (Exception e) {
                            ErrorLogCollector.collect(CommandsTransponder.class, "executeRequest error", e);
                        }
                    }
                    // 如果在插件处中断了，说明命令不需要继续执行
                    if (pluginBreak) {
                        continue;
                    }
                }

                RedisCommand redisCommand = command.getRedisCommand();

                //不支持的命令直接返回NOT_SUPPORT
                if (redisCommand == null || redisCommand.getSupportType() == RedisCommand.CommandSupportType.NOT_SUPPORT) {
                    reply(channelInfo, task, redisCommand, ErrorReply.NOT_SUPPORT, false);
                    ErrorLogCollector.collect(CommandsTransponder.class, "not support command = " + command.getName());
                    hasCommandsSkip = true;
                    continue;
                }

                //subscribe状态下，只能使用指定的命令
                if (channelInfo.isInSubscribe()) {
                    if (redisCommand != RedisCommand.SUBSCRIBE && redisCommand != RedisCommand.SSUBSCRIBE && redisCommand != RedisCommand.PSUBSCRIBE
                            && redisCommand != RedisCommand.UNSUBSCRIBE && redisCommand != RedisCommand.SUNSUBSCRIBE && redisCommand != RedisCommand.PUNSUBSCRIBE
                            && redisCommand != RedisCommand.PING && redisCommand != RedisCommand.QUIT) {
                        ErrorReply errorReply = new ErrorReply("ERR Can't execute '" + redisCommand.strRaw() + "': only (P|S)SUBSCRIBE / (P|S)UNSUBSCRIBE / PING / QUIT are allowed in this context");
                        reply(channelInfo, task, redisCommand, errorReply, false);
                        hasCommandsSkip = true;
                        continue;
                    }
                }

                //sentinel mode
                if (sentinelModeProcessor != null && channelInfo.isFromCport()) {
                    CompletableFuture<Reply> future = sentinelModeProcessor.sentinelCommands(command);
                    if (future != null) {
                        future.thenAccept(task::replyCompleted);
                    }
                    hasCommandsSkip = true;
                    continue;
                }

                //DB类型的命令，before auth
                if (redisCommand.getCommandType() == RedisCommand.CommandType.DB) {
                    if (redisCommand == RedisCommand.PING) {
                        if (channelInfo.isFromCport()) {
                            task.replyCompleted(StatusReply.PONG);
                            hasCommandsSkip = true;
                            continue;
                        }
                    }
                    //quit命令直接断开连接
                    if (redisCommand == RedisCommand.QUIT) {
                        channelInfo.getCtx().close();
                        return;
                    }

                    //auth命令
                    if (redisCommand == RedisCommand.AUTH) {
                        boolean hasBidBgroup = channelInfo.getBid() != null && channelInfo.getBgroup() != null;
                        Reply reply = authCommandProcessor.invokeAuthCommand(channelInfo, command);
                        if (!hasBidBgroup) {
                            boolean pass = checkConnectLimit(channelInfo);
                            if (!pass) return;
                        }
                        task.replyCompleted(reply);
                        hasCommandsSkip = true;
                        continue;
                    }

                    //hello命令
                    if (redisCommand == RedisCommand.HELLO) {
                        boolean hasBidBgroup = channelInfo.getBid() != null && channelInfo.getBgroup() != null;
                        Reply reply = HelloCommandUtil.invokeHelloCommand(channelInfo, authCommandProcessor, command);
                        if (!hasBidBgroup) {
                            boolean pass = checkConnectLimit(channelInfo);
                            if (!pass) return;
                        }
                        task.replyCompleted(reply);
                        hasCommandsSkip = true;
                        continue;
                    }
                }

                //如果需要密码，但是没有auth，则返回NO_AUTH
                if (authCommandProcessor.isPasswordRequired()) {
                    boolean skipAuth = false;
                    if (redisCommand == RedisCommand.CLUSTER) {
                        byte[][] args = command.getObjects();
                        skipAuth = args.length >= 2 && Utils.bytesToString(args[1]).equalsIgnoreCase(RedisKeyword.PROXY_HEARTBEAT.name());
                    }
                    if (redisCommand == RedisCommand.PROXY && channelInfo.isFromCport()) {
                        skipAuth = true;
                    }
                    if (channelInfo.getChannelStats() != ChannelInfo.ChannelStats.AUTH_OK && !skipAuth) {
                        task.replyCompleted(ErrorReply.NO_AUTH);
                        hasCommandsSkip = true;
                        continue;
                    }
                }

                //DB类型的命令，after auth
                if (redisCommand.getCommandType() == RedisCommand.CommandType.DB) {
                    //select命令
                    if (redisCommand == RedisCommand.SELECT) {
                        byte[][] objects = command.getObjects();
                        if (objects.length == 2) {
                            int db = (int) Utils.bytesToNum(command.getObjects()[1]);
                            if (db < 0) {
                                task.replyCompleted(ErrorReply.DB_INDEX_OUT_OF_RANGE);
                            } else {
                                IUpstreamClientTemplate template = factory.tryGet(channelInfo.getBid(), channelInfo.getBgroup());
                                if (template != null && !template.isMultiDBSupport() && db != 0) {
                                    task.replyCompleted(ErrorReply.DB_INDEX_OUT_OF_RANGE);
                                } else {
                                    //需要把之前db的命令先发出去
                                    if (!tasks.isEmpty() && channelInfo.getDb() != db) {
                                        List<Command> list = new ArrayList<>(tasks.size());
                                        for (CommandTask asyncTask : tasks) {
                                            list.add(asyncTask.getCommand());
                                        }
                                        flush(channelInfo.getBid(), channelInfo.getBgroup(), channelInfo.getDb(), tasks, list);
                                        tasks = new ArrayList<>();
                                    }
                                    channelInfo.setDb(db);
                                    task.replyCompleted(StatusReply.OK);
                                }
                            }
                        } else {
                            task.replyCompleted(ErrorReply.argNumWrong(redisCommand));
                        }
                        hasCommandsSkip = true;
                        continue;
                    }

                    //info命令
                    if (redisCommand == RedisCommand.INFO) {
                        CompletableFuture<Reply> future = ProxyInfoUtils.getInfoReply(command, factory);
                        future.thenAccept(task::replyCompleted);
                        hasCommandsSkip = true;
                        continue;
                    }

                    //sentinel不往后发
                    if (redisCommand == RedisCommand.SENTINEL) {
                        task.replyCompleted(ErrorReply.NOT_SUPPORT);
                        hasCommandsSkip = true;
                        continue;
                    }

                    //proxy命令
                    if (redisCommand == RedisCommand.PROXY) {
                        CompletableFuture<Reply> future = proxyCommandProcessor.process(command);
                        future.thenAccept(task::replyCompleted);
                        hasCommandsSkip = true;
                        continue;
                    }

                    //client命令，可以用于选择路由
                    if (redisCommand == RedisCommand.CLIENT) {
                        boolean hasBidBgroup = channelInfo.getBid() != null && channelInfo.getBgroup() != null;
                        Reply reply = ClientCommandUtil.invokeClientCommand(channelInfo, command);
                        if (!hasBidBgroup) {
                            boolean pass = checkConnectLimit(channelInfo);
                            if (!pass) return;
                        }
                        task.replyCompleted(reply);
                        hasCommandsSkip = true;
                        continue;
                    }

                    //ASKING命令直接回OK
                    if (redisCommand == RedisCommand.ASKING) {
                        task.replyCompleted(StatusReply.OK);
                        hasCommandsSkip = true;
                        continue;
                    }

                    //CONFIG命令只支持GET
                    if (redisCommand == RedisCommand.CONFIG) {
                        byte[][] objects = command.getObjects();
                        if (objects.length > 1) {
                            String arg = Utils.bytesToString(objects[1]);
                            if (!arg.equalsIgnoreCase(RedisKeyword.GET.name())) {
                                task.replyCompleted(new ErrorReply("command 'CONFIG' only support GET"));
                                hasCommandsSkip = true;
                                continue;
                            }
                        }
                    }

                    if (redisCommand == RedisCommand.CLUSTER) {
                        //cluster mode
                        if (clusterModeProcessor != null) {
                            CompletableFuture<Reply> future = clusterModeProcessor.clusterCommands(command);
                            future.thenAccept(task::replyCompleted);
                        } else {
                            task.replyCompleted(ErrorReply.NOT_SUPPORT);
                        }
                        hasCommandsSkip = true;
                        continue;
                    }
                }

                if (clusterModeProcessor != null) {
                    if (commands.size() == 1) {//pipeline过来的命令就不move了
                        Reply moveReply = clusterModeProcessor.isCommandMove(command);
                        if (moveReply != null) {
                            task.replyCompleted(moveReply);
                            hasCommandsSkip = true;
                            continue;
                        }
                    }
                }

                //订阅命令特殊处理
                if (redisCommand == RedisCommand.SUBSCRIBE || redisCommand == RedisCommand.PSUBSCRIBE) {
                    channelInfo.setInSubscribe(true);
                }

                //检查是否有路由rewrite
                if (rewriterResult != null) {
                    long bid = rewriterResult.getBid();
                    String bgroup = rewriterResult.getBgroup();
                    if (bid > 0 && bgroup != null) {
                        //判定路由是否有变更
                        if (!Objects.equals(bid, channelInfo.getBid()) || !bgroup.equals(channelInfo.getBgroup())) {
                            //如果路由被变更了，则需要把之前的命令先发出去
                            if (!tasks.isEmpty()) {
                                List<Command> list = new ArrayList<>(tasks.size());
                                for (CommandTask commandTask : tasks) {
                                    list.add(commandTask.getCommand());
                                }
                                flush(channelInfo.getBid(), channelInfo.getBgroup(), channelInfo.getDb(), tasks, list);
                            }
                            //rewrite后的路由直接发出去
                            flush(bid, bgroup, channelInfo.getDb(), Collections.singletonList(task), Collections.singletonList(command));
                            //重置
                            tasks = new ArrayList<>();
                            hasCommandsSkip = true;
                            continue;
                        }
                    }
                }

                tasks.add(task);
            }
            if (tasks.isEmpty()) return;
            if (hasCommandsSkip) {
                commands = new ArrayList<>(tasks.size());
                for (CommandTask commandTask : tasks) {
                    commands.add(commandTask.getCommand());
                }
            }
            //写入到后端
            flush(channelInfo.getBid(), channelInfo.getBgroup(), channelInfo.getDb(), tasks, commands);
        } catch (Exception e) {
            logger.error("commands transponder error, client connect will be force closed, bid = {}, bgroup = {}, addr = {}",
                    channelInfo.getBid(), channelInfo.getBgroup(), channelInfo.getCtx().channel().remoteAddress(), e);
            channelInfo.getCtx().close();
        }
    }

    private void reply(ChannelInfo channelInfo, CommandTask task, RedisCommand redisCommand, Reply reply, boolean fromPlugin) {
        if (channelInfo.isInSubscribe()) {
            CommandTaskQueue taskQueue = channelInfo.getCommandTaskQueue();
            taskQueue.reply(redisCommand, reply, fromPlugin, true);
        } else {
            task.replyCompleted(reply, fromPlugin);
        }
    }

    private boolean checkConnectLimit(ChannelInfo channelInfo) {
        try {
            Long bid = channelInfo.getBid();
            String bgroup = channelInfo.getBgroup();
            if (bid != null && bgroup != null) {
                int currentConnect = ChannelMonitor.bidBgroupConnect(bid, bgroup);
                int threshold = ConnectLimiter.connectThreshold(bid, bgroup);
                if (currentConnect >= threshold) {
                    ChannelHandlerContext ctx = channelInfo.getCtx();
                    channelInfo.setChannelStats(ChannelInfo.ChannelStats.INVALID);
                    ChannelFuture channelFuture = ctx.channel().writeAndFlush(ErrorReply.TOO_MANY_CLIENTS);
                    channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
                        long delayMillis = ConnectLimiter.delayCloseMillis();
                        if (delayMillis > 0) {
                            ExecutorUtils.submitDelayTask(() -> ctx.channel().close(), delayMillis, TimeUnit.MILLISECONDS);
                        } else {
                            ctx.channel().close();
                        }
                    });
                    logger.warn("too many connects, connect will be force closed, bid = {}, bgroup = {}, current = {}, max = {}, consid = {}, client.addr = {}",
                            bid, bgroup, currentConnect, threshold, channelInfo.getConsid(), channelInfo.getCtx().channel().remoteAddress());
                    return false;
                }
                ChannelMonitor.initBidBgroup(bid, bgroup, channelInfo);
            }
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return true;
        }
    }

    private void flush(Long bid, String bgroup, int db, List<CommandTask> tasks, List<Command> commands) {
        try {
            if (!factory.isMultiTenantsSupport() || bid == null || bid <= 0 || bgroup == null) {
                IUpstreamClientTemplate template = factory.getOrInitialize(bid, bgroup);
                flush0(template, bid, bgroup, db, tasks, commands);
                return;
            }
            CompletableFuture<IUpstreamClientTemplate> future = factory.getOrInitializeAsync(bid, bgroup);
            if (future == null) {
                for (CommandTask task : tasks) {
                    task.replyCompleted(ErrorReply.UPSTREAM_NOT_AVAILABLE);
                }
                return;
            }
            future.thenAccept(template -> flush0(template, bid, bgroup, db, tasks, commands));
        } catch (Exception e) {
            ErrorLogCollector.collect(CommandsTransponder.class, "flush commands error", e);
            for (CommandTask task : tasks) {
                task.replyCompleted(ErrorReply.UPSTREAM_NOT_AVAILABLE);
            }
        }
    }

    private void flush0(IUpstreamClientTemplate template, Long bid, String bgroup, int db, List<CommandTask> tasks, List<Command> commands) {
        try {
            if (template == null) {
                for (CommandTask task : tasks) {
                    task.replyCompleted(ErrorReply.UPSTREAM_NOT_AVAILABLE);
                }
            } else {
                List<CompletableFuture<Reply>> futureList;
                try {
                    futureList = template.sendCommand(db, commands);
                } catch (Exception e) {
                    String log = "sendCommand error, bid = " + bid + ", bgroup = " + bgroup + ", ex = " + e;
                    ErrorLogCollector.collect(CommandsTransponder.class, log, e);
                    for (CommandTask task : tasks) {
                        task.replyCompleted(ErrorReply.UPSTREAM_NOT_AVAILABLE);
                    }
                    return;
                }
                for (int i = 0; i < tasks.size(); i++) {
                    CommandTask task = tasks.get(i);
                    CompletableFuture<Reply> completableFuture = futureList.get(i);
                    completableFuture.thenAccept(task::replyCompleted);
                }
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(CommandsTransponder.class, "flush0 commands error", e);
        }
    }

}
