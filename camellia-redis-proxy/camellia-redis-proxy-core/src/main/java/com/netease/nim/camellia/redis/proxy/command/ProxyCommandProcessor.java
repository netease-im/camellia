package com.netease.nim.camellia.redis.proxy.command;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.auth.ClientAuthProvider;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPluginInitResp;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.SysUtils;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/11/21
 */
public class ProxyCommandProcessor {

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuNum(),
            0, TimeUnit.SECONDS, new LinkedBlockingDeque<>(1024),
            new DefaultThreadFactory("proxy-command-invoker"), new ThreadPoolExecutor.AbortPolicy());

    private static final ErrorReply error = new ErrorReply("ERR unknown section for 'PROXY' command");

    private static ProxyPluginInitResp proxyPluginInitResp;
    private static IUpstreamClientTemplateFactory upstreamClientTemplateFactory;
    private static JSONObject transpondConfig = null;
    private static ClientAuthProvider clientAuthProvider;
    private static ProxyClusterModeProcessor proxyClusterModeProcessor;

    public static void updateProxyPluginInitResp(ProxyPluginInitResp proxyPluginInitResp) {
        ProxyCommandProcessor.proxyPluginInitResp = proxyPluginInitResp;
    }

    public static void setUpstreamClientTemplateFactory(IUpstreamClientTemplateFactory upstreamClientTemplateFactory) {
        ProxyCommandProcessor.upstreamClientTemplateFactory = upstreamClientTemplateFactory;
    }

    public static void setClientAuthProvider(ClientAuthProvider clientAuthProvider) {
        ProxyCommandProcessor.clientAuthProvider = clientAuthProvider;
    }

    public static void setProxyClusterModeProcessor(ProxyClusterModeProcessor proxyClusterModeProcessor) {
        ProxyCommandProcessor.proxyClusterModeProcessor = proxyClusterModeProcessor;
    }

    public static void setTranspondConfig(JSONObject transpondConfig) {
        ProxyCommandProcessor.transpondConfig = transpondConfig;
    }

    public static CompletableFuture<Reply> process(Command command) {
        RedisCommand redisCommand = command.getRedisCommand();
        CompletableFuture<Reply> future = new CompletableFuture<>();
        if (redisCommand != RedisCommand.PROXY) {
            future.complete(ErrorReply.NOT_SUPPORT);
            return future;
        }
        final byte[][] args = command.getObjects();
        if (args.length <= 1) {
            future.complete(ErrorReply.SYNTAX_ERROR);
            return future;
        }
        try {
            executor.submit(() -> {
                try {
                    Section section = Section.byValue(Utils.bytesToString(args[1]));
                    if (section == null) {
                        future.complete(error);
                        return;
                    }
                    switch (section) {
                        case INFO:
                            future.complete(info(args));
                            break;
                        case CONFIG:
                            future.complete(config(args));
                            break;
                        default:
                            future.complete(error);
                    }
                } catch (Exception e) {
                    ErrorLogCollector.collect(ProxyCommandProcessor.class, "ProxyCommandProcessor process error", e);
                    future.complete(ErrorReply.SYNTAX_ERROR);
                }
            });
        } catch (Exception e) {
            ErrorLogCollector.collect(ProxyCommandProcessor.class, "ProxyCommandProcessor process error", e);
            future.complete(ErrorReply.TOO_BUSY);
        }
        return future;
    }

    private static Reply info(byte[][] args) {
        boolean simpleClassName = true;
        if (args.length >= 3) {
            simpleClassName = Boolean.parseBoolean(Utils.bytesToString(args[2]));
        }
        StringBuilder builder = new StringBuilder();
        builder.append("version:" + ProxyInfoUtils.VERSION).append("\r\n");
        if (upstreamClientTemplateFactory != null) {
            builder.append("upstream_client_template_factory:").append(className(upstreamClientTemplateFactory, simpleClassName)).append("\r\n");
        } else {
            builder.append("upstream_client_template_factory:").append("\r\n");
        }
        if (transpondConfig != null) {
            builder.append("transpond_config:").append(transpondConfig.toJSONString()).append("\r\n");
        } else {
            builder.append("transpond_config:").append("\r\n");
        }
        if (clientAuthProvider != null) {
            builder.append("client_auth_provider:").append(className(clientAuthProvider, simpleClassName)).append("\r\n");
        } else {
            builder.append("client_auth_provider:").append("\r\n");
        }
        builder.append("cluster_mode_enable:").append(proxyClusterModeProcessor != null).append("\r\n");
        builder.append("proxy_dynamic_conf_loader:").append(className(ProxyDynamicConf.getConfigLoader(), simpleClassName)).append("\r\n");
        builder.append("monitor_enable:").append(ProxyMonitorCollector.isMonitorEnable()).append("\r\n");
        builder.append("command_spend_time_monitor_enable:").append(ProxyMonitorCollector.isCommandSpendTimeMonitorEnable()).append("\r\n");
        builder.append("upstream_redis_spend_time_monitor_enable:").append(ProxyMonitorCollector.isUpstreamRedisSpendTimeMonitorEnable()).append("\r\n");
        if (proxyPluginInitResp == null) {
            builder.append("request_plugins:").append("\r\n");
            builder.append("reply_plugins:").append("\r\n");
        } else {
            List<ProxyPlugin> requestPlugins = proxyPluginInitResp.getRequestPlugins();
            StringBuilder requestPluginsStr = new StringBuilder();
            for (ProxyPlugin plugin : requestPlugins) {
                requestPluginsStr.append(className(plugin, simpleClassName)).append(",");
            }
            if (requestPluginsStr.length() > 0) {
                requestPluginsStr.deleteCharAt(requestPluginsStr.length() - 1);
            }
            builder.append("request_plugins:").append(requestPluginsStr).append("\r\n");
            List<ProxyPlugin> replyPlugins = proxyPluginInitResp.getReplyPlugins();
            StringBuilder replyPluginsStr = new StringBuilder();
            for (ProxyPlugin plugin : replyPlugins) {
                replyPluginsStr.append(className(plugin, simpleClassName)).append(",");
            }
            if (replyPluginsStr.length() > 0) {
                replyPluginsStr.deleteCharAt(replyPluginsStr.length() - 1);
            }
            builder.append("reply_plugins:").append(replyPluginsStr).append("\r\n");
        }
        return new BulkReply(Utils.stringToBytes(builder.toString()));
    }

    private static String className(Object obj, boolean simpleClassName) {
        if (obj == null) {
            return "";
        }
        Class<?> clazz = obj.getClass();
        if (simpleClassName) {
            return clazz.getSimpleName();
        } else {
            return clazz.getName();
        }
    }

    private static Reply config(byte[][] args) {
        if (args.length <= 2) {
            return ErrorReply.SYNTAX_ERROR;
        }
        String type = Utils.bytesToString(args[2]);
        if (type.equalsIgnoreCase("list")) {//proxy config list
            List<MultiBulkReply> list = new ArrayList<>();
            Map<String, String> conf = ProxyDynamicConf.getConf();
            IntegerReply sizeReply = new IntegerReply((long)conf.size());
            for (Map.Entry<String, String> entry : conf.entrySet()) {
                BulkReply key = new BulkReply(Utils.stringToBytes(entry.getKey()));
                BulkReply value = new BulkReply(Utils.stringToBytes(entry.getValue()));
                MultiBulkReply config = new MultiBulkReply(new Reply[] {key, value});
                list.add(config);
            }
            return new MultiBulkReply(new Reply[]{sizeReply, new MultiBulkReply(list.toArray(new Reply[0]))});
        } else if (type.equalsIgnoreCase("reload")) {//proxy config reload
            ProxyDynamicConf.reload();
            return StatusReply.OK;
        } else {
            return ErrorReply.SYNTAX_ERROR;
        }
    }

    private static enum Section {
        INFO,
        CONFIG,
        ;

        public static Section byValue(String section) {
            for (Section value : Section.values()) {
                if (value.name().equalsIgnoreCase(section)) {
                    return value;
                }
            }
            return null;
        }
    }
}
