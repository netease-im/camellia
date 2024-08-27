package com.netease.nim.camellia.redis.proxy.command;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.auth.ClientAuthProvider;
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
import com.netease.nim.camellia.tools.utils.SysUtils;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
            new DefaultThreadFactory("proxy-command-processor"), new ThreadPoolExecutor.AbortPolicy());

    private static final ErrorReply error = new ErrorReply("ERR unknown section for 'PROXY' command");
    private static final ErrorReply md5NotMatch = new ErrorReply("ERR md5 not match");
    private static final ErrorReply notWritable = new ErrorReply("ERR configLoader is not writable");
    private static final ErrorReply writeError = new ErrorReply("ERR configLoader write error");

    private ProxyPluginInitResp proxyPluginInitResp;
    private JSONObject transpondConfig = null;
    private ClientAuthProvider clientAuthProvider;
    private ProxyNodesDiscovery proxyNodesDiscovery;

    public ProxyCommandProcessor() {
        GlobalRedisProxyEnv.addBeforeStartCallback(this::trySyncConfig);
    }

    private void trySyncConfig() {
        String targetNode = ProxyDynamicConf.getString("config.auto.sync.target.proxy.node", null);
        boolean autoSync = ProxyDynamicConf.getBoolean("config.auto.sync.enable", false);
        if (autoSync && targetNode != null) {
            ProxyNode proxyNode = ProxyNode.parseString(targetNode);
            syncFromProxyNode(proxyNode);
        }
    }

    public void updateProxyPluginInitResp(ProxyPluginInitResp proxyPluginInitResp) {
        this.proxyPluginInitResp = proxyPluginInitResp;
    }

    public void setClientAuthProvider(ClientAuthProvider clientAuthProvider) {
        this.clientAuthProvider = clientAuthProvider;
    }

    public void setProxyNodesDiscovery(ProxyNodesDiscovery proxyNodesDiscovery) {
        this.proxyNodesDiscovery = proxyNodesDiscovery;
    }

    public void setTranspondConfig(JSONObject transpondConfig) {
        this.transpondConfig = transpondConfig;
    }

    /**
     * process method
     * @param command command
     * @return reply
     */
    public CompletableFuture<Reply> process(Command command) {
        RedisCommand redisCommand = command.getRedisCommand();
        CompletableFuture<Reply> future = new CompletableFuture<>();
        if (redisCommand != RedisCommand.PROXY) {
            future.complete(Utils.commandNotSupport(redisCommand));
            return future;
        }
        final byte[][] args = command.getObjects();
        if (args.length <= 1) {
            ErrorLogCollector.collect(ProxyCommandProcessor.class, "proxy command syntax error, illegal arg len");
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
                        case NODES:
                            future.complete(nodes());
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

    private Reply info(byte[][] args) {
        boolean simpleClassName = true;
        if (args.length >= 3) {
            simpleClassName = Boolean.parseBoolean(Utils.bytesToString(args[2]));
        }
        StringBuilder builder = new StringBuilder();
        builder.append("camellia_version:" + ProxyInfoUtils.VERSION).append("\r\n");
        IUpstreamClientTemplateFactory upstreamClientTemplateFactory = GlobalRedisProxyEnv.getClientTemplateFactory();
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
        builder.append("proxy_mode:").append(GlobalRedisProxyEnv.proxyMode()).append("\r\n");
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

    private Reply config(byte[][] args) {
        if (args.length <= 2) {
            ErrorLogCollector.collect(ProxyCommandProcessor.class, "proxy command syntax error, illegal arg len");
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
            ProxyNode currentNode = proxyNodesDiscovery.current();
            Reply currentReply = wrapper(currentNode, listMeta());
            //others
            List<ProxyNode> nodes = proxyNodesDiscovery.discovery();
            Reply othersReply = broadcastCommand(nodes, currentNode, new byte[][]{RedisCommand.PROXY.raw(), Section.CONFIG.raw(), Utils.stringToBytes("listMeta")});
            //reply
            return new MultiBulkReply(new Reply[]{currentReply, othersReply});
        } else if (type.equalsIgnoreCase("reload")) {
            return reload();
        } else if (type.equalsIgnoreCase("reloadAll")) {
            //current
            ProxyNode currentNode = proxyNodesDiscovery.current();
            Reply currentReply = wrapper(currentNode, reload());
            //others
            List<ProxyNode> nodes = proxyNodesDiscovery.discovery();
            Reply othersReply = broadcastCommand(nodes, currentNode, new byte[][]{RedisCommand.PROXY.raw(), Section.CONFIG.raw(), Utils.stringToBytes("reload")});
            //reply
            return new MultiBulkReply(new Reply[]{currentReply, othersReply});
        } else if (type.equalsIgnoreCase("write")) {
            if (args.length < 4) {
                ErrorLogCollector.collect(ProxyCommandProcessor.class, "proxy command syntax error, illegal arg len");
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

            ProxyNode currentNode = proxyNodesDiscovery.current();
            List<ProxyNode> nodes = proxyNodesDiscovery.discovery();

            Reply currentReply = wrapper(currentNode, StatusReply.OK);
            Reply othersReply = broadcastCommand(nodes, currentNode, cmd);

            //reply
            return new MultiBulkReply(new Reply[]{currentReply, othersReply});
        } else if (type.equalsIgnoreCase("checkMd5")) {
            ConfigResp configResp = ProxyDynamicConf.getConfigResp();
            String specialConfigMd5 = configResp.getSpecialConfigMd5();
            if (args.length < 4) {
                ErrorLogCollector.collect(ProxyCommandProcessor.class, "proxy command syntax error, illegal arg len");
                return ErrorReply.SYNTAX_ERROR;
            }
            String md5 = Utils.bytesToString(args[3]);
            if (specialConfigMd5.equalsIgnoreCase(md5)) {
                return StatusReply.OK;
            } else {
                return md5NotMatch;
            }
        } else if (type.equalsIgnoreCase("checkMd5All")) {
            ConfigResp configResp = ProxyDynamicConf.getConfigResp();
            String specialConfigMd5 = configResp.getSpecialConfigMd5();
            ProxyNode currentNode = proxyNodesDiscovery.current();
            byte[][] cmd = new byte[][]{RedisCommand.PROXY.raw(), Section.CONFIG.raw(), Utils.stringToBytes("checkMd5"), Utils.stringToBytes(specialConfigMd5)};
            List<ProxyNode> nodes = proxyNodesDiscovery.discovery();

            Reply othersReply = broadcastCommand(nodes, currentNode, cmd);

            return new MultiBulkReply(new Reply[]{new BulkReply(Utils.stringToBytes(specialConfigMd5)), othersReply});
        } else if (type.equalsIgnoreCase("sync")) {
            ConfigResp configResp = ProxyDynamicConf.getConfigResp();
            List<ConfigResp.ConfigEntry> specialConfig = configResp.getSpecialConfig();
            byte[] configContent = configMapToBytes(ConfigResp.toMap(specialConfig));
            return new BulkReply(configContent);
        } else if (type.equalsIgnoreCase("syncFrom")) {
            if (args.length < 4) {
                ErrorLogCollector.collect(ProxyCommandProcessor.class, "proxy command syntax error, illegal arg len");
                return ErrorReply.SYNTAX_ERROR;
            }
            String target = Utils.bytesToString(args[3]);
            ProxyNode proxyNode = ProxyNode.parseString(target);
            if (proxyNode == null) {
                return new ErrorReply("ERR invalid target " + target);
            }
            try {
                syncFromProxyNode(proxyNode);
            } catch (Exception e) {
                logger.error("syncFromProxyNode error, target = {}", proxyNode, e);
                return new ErrorReply("ERR " + e.getMessage());
            }
            return StatusReply.OK;
        } else {
            ErrorLogCollector.collect(ProxyCommandProcessor.class, "proxy command syntax error, unknown type =" + type);
            return ErrorReply.SYNTAX_ERROR;
        }
    }

    private Map<String, String> bytesToConfigMap(byte[] configContent) {
        String str = Utils.bytesToString(configContent);
        JSONObject json = JSONObject.parseObject(str);
        Map<String, String> config = new HashMap<>();
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            config.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return config;
    }

    private byte[] configMapToBytes(Map<String, String> config) {
        JSONObject json = new JSONObject();
        json.putAll(config);
        return Utils.stringToBytes(json.toJSONString());
    }

    private Reply listMeta() {
        ConfigResp configResp = ProxyDynamicConf.getConfigResp();
        return configMetaReply(configResp);
    }

    private Reply reload() {
        ProxyDynamicConf.reload();
        return StatusReply.OK;
    }

    private Reply wrapper(ProxyNode node, Reply reply) {
        return new MultiBulkReply(new Reply[]{nodeReply(node), reply});
    }

    private Reply broadcastCommand(List<ProxyNode> nodes, ProxyNode currentNode, byte[][] args) {
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

    private Reply sendCommand(ProxyNode node, byte[][] args) {
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

    private Reply configMetaReply(ConfigResp configResp) {
        MultiBulkReply initConfigSize = wrapper("init.config.size", configResp.getInitConfig().size());
        MultiBulkReply initConfigMd5 = wrapper("init.config.md5", configResp.getInitConfigMd5());
        MultiBulkReply specialConfigSize = wrapper("special.config.size", configResp.getSpecialConfig().size());
        MultiBulkReply specialConfigMd5 = wrapper("special.config.md5", configResp.getSpecialConfigMd5());
        MultiBulkReply configSize = wrapper("all.config.size", configResp.getAllConfig().size());
        MultiBulkReply configMd5 = wrapper("all.config.md5", configResp.getAllConfigMd5());
        return new MultiBulkReply(new Reply[]{initConfigSize, initConfigMd5, specialConfigSize, specialConfigMd5, configSize, configMd5});
    }

    private MultiBulkReply wrapper(String config, Object value) {
        return new MultiBulkReply(new Reply[]{new BulkReply(Utils.stringToBytes(config)), new BulkReply(Utils.stringToBytes(String.valueOf(value)))});
    }

    private Reply nodeReply(ProxyNode node) {
        return new BulkReply(Utils.stringToBytes(node.toString()));
    }

    private Reply nodes() {
        //current
        ProxyNode currentNode = proxyNodesDiscovery.current();
        Reply current = nodeReply(currentNode);
        //list
        List<Reply> list = new ArrayList<>();
        List<ProxyNode> nodes = proxyNodesDiscovery.discovery();
        for (ProxyNode node : nodes) {
            list.add(nodeReply(node));
        }
        return new MultiBulkReply(new Reply[]{current, new MultiBulkReply(list.toArray(new Reply[0]))});
    }

    private void syncFromProxyNode(ProxyNode targetNode) {
        if (targetNode == null) {
            throw new IllegalArgumentException("syncFromProxyNode error, targetNode is null");
        }
        if (proxyNodesDiscovery != null && targetNode.equals(proxyNodesDiscovery.current())) {
            logger.warn("skip syncFromProxyNode, targetNode = {}, current = {}", targetNode, proxyNodesDiscovery.current());
            return;
        }
        byte[][] cmd = new byte[][] {
                RedisCommand.PROXY.raw(),
                Section.CONFIG.raw(),
                Utils.stringToBytes("sync")
        };
        Reply reply = sendCommand(targetNode, cmd);
        if (reply instanceof ErrorReply) {
            throw new IllegalArgumentException("syncFromProxyNode error, targetNode = " + targetNode + ", reply = " + reply);
        }
        if (reply instanceof BulkReply) {
            byte[] raw = ((BulkReply) reply).getRaw();
            Map<String, String> map = bytesToConfigMap(raw);
            ProxyDynamicConfLoader configLoader = ProxyDynamicConf.getConfigLoader();
            if (configLoader instanceof WritableProxyDynamicConfLoader) {
                boolean ok = ((WritableProxyDynamicConfLoader) configLoader).write(map);
                if (ok) {
                    ProxyDynamicConf.reload();
                    logger.info("syncFromProxyNode success, targetNode = {}, config.size = {}", targetNode, map.size());
                    return;
                }
            } else {
                throw new IllegalArgumentException("syncFromProxyNode error, ProxyDynamicConfLoader not writable");
            }
        }
        logger.error("syncFromProxyNode error, target = {}, reply = {}", targetNode, reply);
        throw new IllegalArgumentException("syncFromProxyNode error");
    }

    private static enum Section {
        INFO,
        CONFIG,
        NODES,
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

}
