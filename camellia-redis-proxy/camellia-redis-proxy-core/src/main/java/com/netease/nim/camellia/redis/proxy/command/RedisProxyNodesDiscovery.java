package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.conf.ServerConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.sentinel.ProxySentinelModeProcessor;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2023/12/1
 */
public class RedisProxyNodesDiscovery extends AbstractProxyNodesDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(RedisProxyNodesDiscovery.class);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("redis-proxy-nodes-heartbeat"));

    private final String redisUrl;
    private final String heartbeatKey;
    private List<ProxyNode> proxyNodeList = new ArrayList<>();

    public RedisProxyNodesDiscovery(ProxyClusterModeProcessor proxyClusterModeProcessor, ProxySentinelModeProcessor proxySentinelModeProcessor) {
        super(proxyClusterModeProcessor, proxySentinelModeProcessor);
        this.redisUrl = ProxyDynamicConf.getString("proxy.nodes.discovery.redis.url", "");
        if (redisUrl == null || redisUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("'proxy.nodes.discovery.redis.url' is empty");
        }
        String heartbeatKeyPrefix = ProxyDynamicConf.getString("proxy.nodes.discovery.redis.heartbeat.key.prefix", "camellia_redis_proxy_heartbeat");
        this.heartbeatKey = heartbeatKeyPrefix + "|" + ServerConf.getApplicationName();
        GlobalRedisProxyEnv.getClientTemplateFactory().getEnv().getClientFactory().get(redisUrl);
        GlobalRedisProxyEnv.addAfterStartCallback(this::init);
    }

    private void init() {
        boolean success = heartbeat();
        if (!success) {
            throw new IllegalArgumentException("proxy nodes redis heartbeat fail");
        }
        int intervalSeconds = ProxyDynamicConf.getInt("proxy.nodes.discovery.redis.heartbeat.interval.seconds", 5);
        scheduler.scheduleAtFixedRate(this::heartbeat, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private boolean heartbeat() {
        try {
            IUpstreamClient upstreamClient = GlobalRedisProxyEnv.getClientTemplateFactory().getEnv().getClientFactory().get(redisUrl);
            byte[][] zadd = new byte[][] {
                    RedisCommand.ZADD.raw(),
                    Utils.stringToBytes(heartbeatKey),
                    Utils.stringToBytes(String.valueOf(System.currentTimeMillis())),
                    Utils.stringToBytes(current().toString())
            };
            int heartbeatTimeoutSeconds = ProxyDynamicConf.getInt("proxy.nodes.discovery.redis.heartbeat.timeout.seconds", 30);
            byte[][] zremrangeByScore = new byte[][] {
                    RedisCommand.ZREMRANGEBYSCORE.raw(),
                    Utils.stringToBytes(heartbeatKey),
                    Utils.stringToBytes("0"),
                    Utils.stringToBytes(String.valueOf(System.currentTimeMillis() - 1000L * heartbeatTimeoutSeconds))
            };
            byte[][] expire = new byte[][] {
                    RedisCommand.EXPIRE.raw(),
                    Utils.stringToBytes(heartbeatKey),
                    Utils.stringToBytes(String.valueOf(heartbeatTimeoutSeconds * 3))
            };
            byte[][] zrange = new byte[][] {
                    RedisCommand.ZRANGE.raw(),
                    Utils.stringToBytes(heartbeatKey),
                    Utils.stringToBytes("0"),
                    Utils.stringToBytes("-1")
            };
            List<Command> commandList = new ArrayList<>();
            commandList.add(new Command(zadd));
            commandList.add(new Command(zremrangeByScore));
            commandList.add(new Command(expire));
            commandList.add(new Command(zrange));
            List<CompletableFuture<Reply>> futureList = new ArrayList<>();
            CompletableFuture<Reply> future1 = new CompletableFuture<>();
            CompletableFuture<Reply> future2 = new CompletableFuture<>();
            CompletableFuture<Reply> future3 = new CompletableFuture<>();
            CompletableFuture<Reply> future4 = new CompletableFuture<>();
            futureList.add(future1);
            futureList.add(future2);
            futureList.add(future3);
            futureList.add(future4);
            upstreamClient.sendCommand(-1, commandList, futureList);
            boolean success = true;
            Reply reply1 = future1.get(10, TimeUnit.SECONDS);
            if (reply1 instanceof ErrorReply) {
                success = false;
                logger.error("heartbeat, zadd error, reply = {}", reply1);
            }
            Reply reply2 = future2.get(10, TimeUnit.SECONDS);
            if (reply2 instanceof ErrorReply) {
                success = false;
                logger.error("heartbeat, ZREMRANGEBYSCORE error, reply = {}", reply2);
            }
            Reply reply3 = future3.get(10, TimeUnit.SECONDS);
            if (reply3 instanceof ErrorReply) {
                success = false;
                logger.error("heartbeat, EXPIRE error, reply = {}", reply3);
            }
            Reply reply4 = future4.get(10, TimeUnit.SECONDS);
            if (reply4 instanceof MultiBulkReply && success) {
                List<ProxyNode> proxyNodes = new ArrayList<>();
                Reply[] replies = ((MultiBulkReply) reply4).getReplies();
                for (Reply reply : replies) {
                    if (reply instanceof BulkReply) {
                        String str = Utils.bytesToString((((BulkReply) reply).getRaw()));
                        ProxyNode proxyNode = ProxyNode.parseString(str);
                        if (proxyNode == null) {
                            logger.warn("proxy node parse error, str = {}", str);
                            continue;
                        }
                        proxyNodes.add(proxyNode);
                    }
                }
                Collections.sort(proxyNodes);
                if (!this.proxyNodeList.equals(proxyNodes)) {
                    this.proxyNodeList = proxyNodes;
                    logger.info("proxy node list update, list = {}", this.proxyNodeList);
                }
            } else if (reply4 instanceof ErrorReply) {
                success = false;
                logger.error("heartbeat, ZRANGE error, reply = {}", reply4);
            }
            if (!success) {
                logger.error("heartbeat error, redisUrl = {}", redisUrl);
            }
            return success;
        } catch (Exception e) {
            logger.error("heartbeat error", e);
            return false;
        }
    }

    @Override
    public List<ProxyNode> discovery() {
        List<ProxyNode> proxyNodes = super.discovery();
        if (proxyNodes != null) {
            return proxyNodes;
        }
        return proxyNodeList;
    }
}
