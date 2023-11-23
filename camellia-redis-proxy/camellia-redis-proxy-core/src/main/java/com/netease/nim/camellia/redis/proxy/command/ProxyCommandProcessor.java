package com.netease.nim.camellia.redis.proxy.command;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.auth.ClientAuthProvider;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.conf.ConfigResp;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConfLoader;
import com.netease.nim.camellia.redis.proxy.conf.WritableProxyDynamicConfLoader;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPluginInitResp;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.InetUtils;
import com.netease.nim.camellia.tools.utils.SysUtils;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2023/11/21
 */
public class ProxyCommandProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ProxyCommandProcessor.class);

    private static final int executorSize;
    static {
        executorSize = Math.max(4, Math.min(SysUtils.getCpuNum(), 8));
    }
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(executorSize, executorSize,
            0, TimeUnit.SECONDS, new LinkedBlockingDeque<>(1024),
            new DefaultThreadFactory("proxy-command-invoker"), new ThreadPoolExecutor.AbortPolicy());

    private static final ErrorReply error = new ErrorReply("ERR unknown section for 'PROXY' command");
    private static final ErrorReply notWritable = new ErrorReply("ERR configLoader is not writable");
    private static final ErrorReply writeError = new ErrorReply("ERR configLoader write error");

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

    /**
     * process method
     * @param command command
     * @return reply
     */
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
                        case SERVERS:
                            future.complete(servers());
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
            builder.append("upstream_client_template_factory:").append(Utils.className(upstreamClientTemplateFactory, simpleClassName)).append("\r\n");
        } else {
            builder.append("upstream_client_template_factory:").append("\r\n");
        }
        if (transpondConfig != null) {
            builder.append("transpond_config:").append(transpondConfig.toJSONString()).append("\r\n");
        } else {
            builder.append("transpond_config:").append("\r\n");
        }
        if (clientAuthProvider != null) {
            builder.append("client_auth_provider:").append(Utils.className(clientAuthProvider, simpleClassName)).append("\r\n");
        } else {
            builder.append("client_auth_provider:").append("\r\n");
        }
        builder.append("cluster_mode_enable:").append(proxyClusterModeProcessor != null).append("\r\n");
        builder.append("proxy_dynamic_conf_loader:").append(Utils.className(ProxyDynamicConf.getConfigLoader(), simpleClassName)).append("\r\n");
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
                requestPluginsStr.append(Utils.className(plugin, simpleClassName)).append(",");
            }
            if (requestPluginsStr.length() > 0) {
                requestPluginsStr.deleteCharAt(requestPluginsStr.length() - 1);
            }
            builder.append("request_plugins:").append(requestPluginsStr).append("\r\n");
            List<ProxyPlugin> replyPlugins = proxyPluginInitResp.getReplyPlugins();
            StringBuilder replyPluginsStr = new StringBuilder();
            for (ProxyPlugin plugin : replyPlugins) {
                replyPluginsStr.append(Utils.className(plugin, simpleClassName)).append(",");
            }
            if (replyPluginsStr.length() > 0) {
                replyPluginsStr.deleteCharAt(replyPluginsStr.length() - 1);
            }
            builder.append("reply_plugins:").append(replyPluginsStr).append("\r\n");
        }
        return new BulkReply(Utils.stringToBytes(builder.toString()));
    }

    private static Reply config(byte[][] args) {
        if (args.length <= 2) {
            return ErrorReply.SYNTAX_ERROR;
        }
        String type = Utils.bytesToString(args[2]);
        if (type.equalsIgnoreCase("list")) {//proxy config list
            ConfigResp configResp = ProxyDynamicConf.getConfigResp();
            //meta
            Reply configMeta = configMetaReply(configResp);
            //detail
            List<MultiBulkReply> list = new ArrayList<>();
            for (ConfigResp.ConfigEntry config : configResp.getAllConfig()) {
                BulkReply key = new BulkReply(Utils.stringToBytes(config.getKey()));
                BulkReply value = new BulkReply(Utils.stringToBytes(config.getValue()));
                list.add(new MultiBulkReply(new Reply[]{key, value}));
            }
            return new MultiBulkReply(new Reply[]{configMeta, new MultiBulkReply(list.toArray(new Reply[0]))});
        } else if (type.equalsIgnoreCase("listMeta")) {
            return listMeta();
        } else if (type.equalsIgnoreCase("listMetaAll")) {
            //current
            ProxyNode currentNode = getCurrentNode();
            Reply currentReply = wrapper(currentNode, listMeta());
            //others
            List<ProxyNode> nodes = getNodes();
            Reply othersReply = broadcastCommand(nodes, currentNode, new byte[][]{RedisCommand.PROXY.raw(), Section.CONFIG.raw(), Utils.stringToBytes("listMeta")});
            //reply
            return new MultiBulkReply(new Reply[]{currentReply, othersReply});
        } else if (type.equalsIgnoreCase("reload")) {
            return reload();
        } else if (type.equalsIgnoreCase("reloadAll")) {
            //current
            ProxyNode currentNode = getCurrentNode();
            Reply currentReply = wrapper(currentNode, reload());
            //others
            List<ProxyNode> nodes = getNodes();
            Reply othersReply = broadcastCommand(nodes, currentNode, new byte[][]{RedisCommand.PROXY.raw(), Section.CONFIG.raw(), Utils.stringToBytes("reload")});
            //reply
            return new MultiBulkReply(new Reply[]{currentReply, othersReply});
        } else if (type.equalsIgnoreCase("write")) {
            if (args.length < 4) {
                return ErrorReply.SYNTAX_ERROR;
            }
            byte[] configContent = args[3];
            ProxyDynamicConfLoader configLoader = ProxyDynamicConf.getConfigLoader();
            if (configLoader instanceof WritableProxyDynamicConfLoader) {
                Map<String, String> map = bytesToConfigMap(configContent);
                boolean ok = ((WritableProxyDynamicConfLoader) configLoader).write(map);
                if (ok) {
                    ProxyDynamicConf.reload();
                    return StatusReply.OK;
                } else {
                    return writeError;
                }
            }
            return notWritable;
        } else if (type.equalsIgnoreCase("broadcast")) {
            ProxyDynamicConf.reload();
            ConfigResp configResp = ProxyDynamicConf.getConfigResp();
            List<ConfigResp.ConfigEntry> specialConfig = configResp.getSpecialConfig();
            byte[] configContent = configMapToBytes(ConfigResp.toMap(specialConfig));
            byte[][] cmd = new byte[][]{RedisCommand.PROXY.raw(), Section.CONFIG.raw(), Utils.stringToBytes("write"), configContent};

            ProxyNode currentNode = getCurrentNode();
            List<ProxyNode> nodes = getNodes();

            Reply currentReply = wrapper(currentNode, StatusReply.OK);
            Reply othersReply = broadcastCommand(nodes, currentNode, cmd);

            //reply
            return new MultiBulkReply(new Reply[]{currentReply, othersReply});
        } else {
            return ErrorReply.SYNTAX_ERROR;
        }
    }

    private static Map<String, String> bytesToConfigMap(byte[] configContent) {
        String str = Utils.bytesToString(configContent);
        JSONObject json = JSONObject.parseObject(str);
        Map<String, String> config = new HashMap<>();
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            config.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return config;
    }

    private static byte[] configMapToBytes(Map<String, String> config) {
        JSONObject json = new JSONObject();
        json.putAll(config);
        return Utils.stringToBytes(json.toJSONString());
    }

    private static Reply listMeta() {
        ConfigResp configResp = ProxyDynamicConf.getConfigResp();
        return configMetaReply(configResp);
    }

    private static Reply reload() {
        ProxyDynamicConf.reload();
        return StatusReply.OK;
    }

    private static Reply wrapper(ProxyNode node, Reply reply) {
        return new MultiBulkReply(new Reply[]{nodeReply(node), reply});
    }

    private static Reply broadcastCommand(List<ProxyNode> nodes, ProxyNode currentNode, byte[][] args) {
        List<MultiBulkReply> list = new ArrayList<>();
        for (ProxyNode node : nodes) {
            if (node.equals(currentNode)) {
                continue;
            }
            Reply reply = sendCommand(node, args);
            list.add(new MultiBulkReply(new Reply[]{nodeReply(node), reply}));
        }
        return new MultiBulkReply(list.toArray(new Reply[0]));
    }

    private static Reply sendCommand(ProxyNode node, byte[][] args) {
        RedisConnection connection = null;
        try {
            if (node.getCport() <= 0) {
                return new ErrorReply("ERR target proxy node cport disabled");
            }
            connection = RedisConnectionHub.getInstance().newConnection(null, node.getHost(), node.getCport(), null, null);
            CompletableFuture<Reply> future = connection.sendCommand(args);
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("target proxy node command invoke error, node = {}", node, e);
            return new ErrorReply("ERR target proxy node command invoke error");
        } finally {
            if (connection != null) {
                connection.stop(true);
            }
        }
    }

    private static Reply configMetaReply(ConfigResp configResp) {
        MultiBulkReply initConfigSize = wrapper("init.config.size", configResp.getInitConfig().size());
        MultiBulkReply initConfigMd5 = wrapper("init.config.md5", configResp.getInitConfigMd5());
        MultiBulkReply specialConfigSize = wrapper("special.config.size", configResp.getSpecialConfig().size());
        MultiBulkReply specialConfigMd5 = wrapper("special.config.md5", configResp.getSpecialConfigMd5());
        MultiBulkReply configSize = wrapper("all.config.size", configResp.getAllConfig().size());
        MultiBulkReply configMd5 = wrapper("all.config.md5", configResp.getAllConfigMd5());
        return new MultiBulkReply(new Reply[]{initConfigSize, initConfigMd5, specialConfigSize, specialConfigMd5, configSize, configMd5});
    }

    private static MultiBulkReply wrapper(String config, Object value) {
        return new MultiBulkReply(new Reply[]{new BulkReply(Utils.stringToBytes(config)), new BulkReply(Utils.stringToBytes(String.valueOf(value)))});
    }

    private static Reply nodeReply(ProxyNode node) {
        return new BulkReply(Utils.stringToBytes(node.toString()));
    }

    private static Reply servers() {
        if (proxyClusterModeProcessor != null) {
            //开关
            BulkReply redisClusterModeEnable = new BulkReply(Utils.stringToBytes("redis_cluster_mode_enable:true"));
            //current
            ProxyNode currentNode = proxyClusterModeProcessor.getCurrentNode();
            Reply current = nodeReply(currentNode);
            //list
            List<Reply> list = new ArrayList<>();
            List<ProxyNode> onlineNodes = proxyClusterModeProcessor.getOnlineNodes();
            for (ProxyNode onlineNode : onlineNodes) {
                list.add(nodeReply(onlineNode));
            }
            return new MultiBulkReply(new Reply[]{redisClusterModeEnable, current, new MultiBulkReply(list.toArray(new Reply[0]))});
        } else {
            //开关
            BulkReply redisClusterModeEnable = new BulkReply(Utils.stringToBytes("redis_cluster_mode_enable:false"));
            //current
            ProxyNode currentNode = getCurrentNode();
            Reply current = nodeReply(currentNode);
            //list
            List<Reply> list = new ArrayList<>();
            List<ProxyNode> nodes = getNodes();
            boolean containsCurrent = false;
            for (ProxyNode node : nodes) {
                if (node.equals(currentNode)) {
                    containsCurrent = true;
                }
                list.add(nodeReply(node));
            }
            if (!containsCurrent) {
                list.add(nodeReply(currentNode));
            }
            return new MultiBulkReply(new Reply[]{redisClusterModeEnable, current, new MultiBulkReply(list.toArray(new Reply[0]))});
        }
    }

    private static enum Section {
        INFO,
        CONFIG,
        SERVERS,
        ;

        public static Section byValue(String section) {
            for (Section value : Section.values()) {
                if (value.name().equalsIgnoreCase(section)) {
                    return value;
                }
            }
            return null;
        }

        public byte[] raw() {
            return name().getBytes(StandardCharsets.UTF_8);
        }
    }

    private static ProxyNode getCurrentNode() {
        String currentNodeHost = ProxyDynamicConf.getString("proxy.node.current.host", null);
        if (currentNodeHost == null) {
            InetAddress inetAddress = InetUtils.findFirstNonLoopbackAddress();
            if (inetAddress == null) {
                currentNodeHost = "127.0.0.1";
            } else {
                currentNodeHost = inetAddress.getHostAddress();
            }
        }
        ProxyNode currentNode = new ProxyNode();
        currentNode.setHost(currentNodeHost);
        currentNode.setPort(GlobalRedisProxyEnv.getPort());
        currentNode.setCport(GlobalRedisProxyEnv.getCport());
        return currentNode;
    }

    private static List<ProxyNode> getNodes() {
        String string = ProxyDynamicConf.getString("proxy.nodes", "");
        List<ProxyNode> list = new ArrayList<>();
        if (string != null && string.trim().length() > 0) {
            string = string.trim();
            String[] split = string.split(",");
            for (String str : split) {
                ProxyNode proxyNode = ProxyNode.parseString(str);
                if (proxyNode == null) {
                    continue;
                }
                list.add(proxyNode);
            }
        }
        return list;
    }

}
