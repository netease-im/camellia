package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.auth.AuthCommandProcessor;
import com.netease.nim.camellia.redis.proxy.auth.ClientCommandUtil;
import com.netease.nim.camellia.redis.proxy.auth.ConnectLimiter;
import com.netease.nim.camellia.redis.proxy.auth.HelloCommandUtil;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientHub;
import com.netease.nim.camellia.redis.proxy.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.ChannelMonitor;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.upstream.AsyncCamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.AsyncCamelliaRedisTemplateChooser;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 * Created by caojiajun on 2021/5/26
 */
public class CommandsTransponder {

    private static final Logger logger = LoggerFactory.getLogger(CommandsTransponder.class);

    private final AuthCommandProcessor authCommandProcessor;
    private final ProxyClusterModeProcessor clusterModeProcessor;
    private final AsyncCamelliaRedisTemplateChooser chooser;
    private final ProxyPluginFactory proxyPluginFactory;

    private boolean eventLoopSetSuccess = false;

    private ProxyPluginInitResp proxyPluginInitResp;

    public CommandsTransponder(AsyncCamelliaRedisTemplateChooser chooser, CommandInvokeConfig commandInvokeConfig) {
        this.chooser = chooser;
        this.authCommandProcessor = commandInvokeConfig.getAuthCommandProcessor();
        this.clusterModeProcessor = commandInvokeConfig.getClusterModeProcessor();
        this.proxyPluginFactory = commandInvokeConfig.getProxyPluginFactory();
        this.proxyPluginInitResp = proxyPluginFactory.initPlugins();
        proxyPluginFactory.registerPluginUpdate(() -> proxyPluginInitResp = proxyPluginFactory.initPlugins());
    }

    public void transpond(ChannelInfo channelInfo, List<Command> commands) {
        if (!eventLoopSetSuccess) {
            RedisClientHub.updateEventLoop(channelInfo.getCtx().channel().eventLoop());
            eventLoopSetSuccess = true;
        }
        try {
            boolean hasCommandsSkip = false;
            AsyncTaskQueue taskQueue = channelInfo.getAsyncTaskQueue();

            if (logger.isDebugEnabled()) {
                List<String> commandNameList = new ArrayList<>(commands.size());
                for (Command command : commands) {
                    commandNameList.add(command.getName());
                }
                logger.debug("receive commands, commands.size = {}, consid = {}, commands = {}",
                        commands.size(), taskQueue.getChannelInfo().getConsid(), commandNameList);
            }

            List<AsyncTask> tasks = new ArrayList<>(commands.size());
            ChannelHandlerContext ctx = channelInfo.getCtx();

            for (Command command : commands) {
                //设置channelInfo
                command.setChannelInfo(channelInfo);

                //任务队列
                AsyncTask task = new AsyncTask(taskQueue, command, proxyPluginInitResp.getReplyPlugins());
                boolean add = taskQueue.add(task);
                if (!add) {
                    taskQueue.clear();
                    logger.warn("AsyncTaskQueue full, client connect will be disconnect, remote.ip = {}", ctx.channel().remoteAddress());
                    ctx.writeAndFlush(ErrorReply.TOO_BUSY).addListener((ChannelFutureListener) future -> ctx.close());
                    return;
                }

                List<ProxyPlugin> requestPlugins = proxyPluginInitResp.getRequestPlugins();
                if (!requestPlugins.isEmpty()) {
                    //执行插件
                    ProxyRequest request = new ProxyRequest(command, chooser);
                    for (ProxyPlugin plugin : proxyPluginInitResp.getRequestPlugins()) {
                        try {
                            ProxyPluginResponse response = plugin.executeRequest(request);
                            if (!response.isPass()) {
                                task.replyCompleted(response.getReply(), true);
                                hasCommandsSkip = true;
                                break;
                            }
                        } catch (Exception e) {
                            ErrorLogCollector.collect(CommandsTransponder.class, "executeRequest error", e);
                        }
                    }
                }
                if (hasCommandsSkip) {
                    continue;
                }

                RedisCommand redisCommand = command.getRedisCommand();
                //ping命令直接返回
                if (redisCommand == RedisCommand.PING) {
                    task.replyCompleted(StatusReply.PONG);
                    hasCommandsSkip = true;
                    continue;
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

                //如果需要密码，但是没有auth，则返回NO_AUTH
                if (authCommandProcessor.isPasswordRequired()) {
                    boolean skipAuth = false;
                    if (redisCommand == RedisCommand.CLUSTER) {
                        byte[][] args = command.getObjects();
                        skipAuth = args.length >= 2 && Utils.bytesToString(args[1]).equalsIgnoreCase(RedisKeyword.PROXY_HEARTBEAT.name());
                    }
                    if (channelInfo.getChannelStats() != ChannelInfo.ChannelStats.AUTH_OK && !skipAuth) {
                        task.replyCompleted(ErrorReply.NO_AUTH);
                        hasCommandsSkip = true;
                        continue;
                    }
                }

                //select命令只支持select 0
                if (redisCommand == RedisCommand.SELECT) {
                    byte[][] objects = command.getObjects();
                    if (objects.length == 2) {
                        if ("0".equals(Utils.bytesToString(command.getObjects()[1]))) {
                            task.replyCompleted(StatusReply.OK);
                        } else {
                            task.replyCompleted(new ErrorReply("ERR DB index is out of range"));
                        }
                    } else {
                        task.replyCompleted(ErrorReply.argNumWrong(redisCommand));
                    }
                    hasCommandsSkip = true;
                    continue;
                }

                //info命令
                if (redisCommand == RedisCommand.INFO) {
                    CompletableFuture<Reply> future = ProxyInfoUtils.getInfoReply(command, chooser);
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

                //quit命令直接断开连接
                if (redisCommand == RedisCommand.QUIT) {
                    channelInfo.getCtx().close();
                    return;
                }

                if (redisCommand == RedisCommand.ASKING) {
                    task.replyCompleted(StatusReply.OK);
                    hasCommandsSkip = true;
                    continue;
                }

                //cluster mode
                if (clusterModeProcessor != null) {
                    if (redisCommand == RedisCommand.CLUSTER) {
                        CompletableFuture<Reply> future = clusterModeProcessor.clusterCommands(command);
                        future.thenAccept(task::replyCompleted);
                        hasCommandsSkip = true;
                        continue;
                    }
                    if (commands.size() == 1) {//pipeline过来的命令就不move了
                        Reply moveReply = clusterModeProcessor.isCommandMove(command);
                        if (moveReply != null) {
                            task.replyCompleted(moveReply);
                            hasCommandsSkip = true;
                            continue;
                        }
                    }
                } else {
                    if (redisCommand == RedisCommand.CLUSTER) {
                        task.replyCompleted(ErrorReply.NOT_SUPPORT);
                        hasCommandsSkip = true;
                        continue;
                    }
                }

                //订阅命令需要特殊处理
                if (redisCommand == RedisCommand.SUBSCRIBE || redisCommand == RedisCommand.PSUBSCRIBE) {
                    channelInfo.setInSubscribe(true);
                }
                if (channelInfo.isInSubscribe()) {
                    if (redisCommand != RedisCommand.SUBSCRIBE && redisCommand != RedisCommand.PSUBSCRIBE
                            && redisCommand != RedisCommand.UNSUBSCRIBE && redisCommand != RedisCommand.PUNSUBSCRIBE) {
                        taskQueue.reply(redisCommand, new ErrorReply("Command " + redisCommand.strRaw() + " not allowed while subscribed. Allowed commands are: [PSUBSCRIBE, QUIT, PUNSUBSCRIBE, SUBSCRIBE, UNSUBSCRIBE]"));
                        hasCommandsSkip = true;
                        continue;
                    }
                }

                //其他不支持的命令直接返回NOT_SUPPORT
                if (redisCommand == null || redisCommand.getSupportType() == RedisCommand.CommandSupportType.NOT_SUPPORT) {
                    task.replyCompleted(ErrorReply.NOT_SUPPORT);
                    hasCommandsSkip = true;
                    continue;
                }

                tasks.add(task);
            }
            if (tasks.isEmpty()) return;
            if (hasCommandsSkip) {
                commands = new ArrayList<>(tasks.size());
                for (AsyncTask asyncTask : tasks) {
                    commands.add(asyncTask.getCommand());
                }
            }
            //写入到后端
            flush(channelInfo.getBid(), channelInfo.getBgroup(), tasks, commands);
        } catch (Exception e) {
            logger.error("commands transponder error, client connect will be force closed, bid = {}, bgroup = {}, addr = {}",
                    channelInfo.getBid(), channelInfo.getBgroup(), channelInfo.getCtx().channel().remoteAddress(), e);
            channelInfo.getCtx().close();
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
                    ctx.writeAndFlush(ErrorReply.TOO_MANY_CONNECTS)
                            .addListener((ChannelFutureListener) future -> ctx.close());
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

    private void flush(Long bid, String bgroup, List<AsyncTask> tasks, List<Command> commands) {
        try {
            if (!chooser.isMultiTenancySupport() || bid == null || bid <= 0 || bgroup == null) {
                AsyncCamelliaRedisTemplate template = chooser.choose(bid, bgroup);
                flush0(template, bid, bgroup, tasks, commands);
                return;
            }
            CompletableFuture<AsyncCamelliaRedisTemplate> future = chooser.chooseAsync(bid, bgroup);
            if (future == null) {
                for (AsyncTask task : tasks) {
                    task.replyCompleted(ErrorReply.NOT_AVAILABLE);
                }
                return;
            }
            future.thenAccept(template -> flush0(template, bid, bgroup, tasks, commands));
        } catch (Exception e) {
            ErrorLogCollector.collect(CommandsTransponder.class, "flush commands error", e);
            for (AsyncTask task : tasks) {
                task.replyCompleted(ErrorReply.NOT_AVAILABLE);
            }
        }
    }

    private void flush0(AsyncCamelliaRedisTemplate template, Long bid, String bgroup, List<AsyncTask> tasks, List<Command> commands) {
        try {
            if (template == null) {
                for (AsyncTask task : tasks) {
                    task.replyCompleted(ErrorReply.NOT_AVAILABLE);
                }
            } else {
                List<CompletableFuture<Reply>> futureList;
                try {
                    futureList = template.sendCommand(commands);
                } catch (Exception e) {
                    String log = "AsyncCamelliaRedisTemplateChooser sendCommand error"
                            + ", bid = " + bid + ", bgroup = " + bgroup + ", ex = " + e;
                    ErrorLogCollector.collect(CommandsTransponder.class, log, e);
                    for (AsyncTask task : tasks) {
                        task.replyCompleted(ErrorReply.NOT_AVAILABLE);
                    }
                    return;
                }
                for (int i = 0; i < tasks.size(); i++) {
                    AsyncTask task = tasks.get(i);
                    CompletableFuture<Reply> completableFuture = futureList.get(i);
                    completableFuture.thenAccept(task::replyCompleted);
                }
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(CommandsTransponder.class, "flush commands error", e);
        }
    }

}
