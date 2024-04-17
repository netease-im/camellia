package com.netease.nim.camellia.redis.proxy.upstream.kv;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.base.resource.RedisKvResource;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.netty.NettyTransportMode;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.redis.proxy.upstream.UpstreamRedisClientTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commanders;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.RedisTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.CacheConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KeyStruct;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KvConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.DefaultKeyMetaServer;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMetaServer;
import com.netease.nim.camellia.redis.proxy.upstream.utils.CompletableFutureUtils;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.executor.CamelliaHashedExecutor;
import com.netease.nim.camellia.tools.utils.SysUtils;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/4/17
 */
public class RedisKvUpstreamClient implements IUpstreamClient {

    private final Resource resource;
    private final String namespace;
    private CamelliaHashedExecutor executor;
    private Commanders commanders;
    private EventLoopGroup eventLoopGroup;

    public RedisKvUpstreamClient(RedisKvResource resource) {
        this.resource = resource;
        this.namespace = resource.getNamespace();
    }

    @Override
    public void sendCommand(int db, List<Command> commands, List<CompletableFuture<Reply>> futureList) {
        if (db > 0) {
            for (CompletableFuture<Reply> future : futureList) {
                future.complete(ErrorReply.DB_INDEX_OUT_OF_RANGE);
            }
            return;
        }
        for (int i=0; i<commands.size(); i++) {
            Command command = commands.get(i);
            CompletableFuture<Reply> future = futureList.get(i);
            RedisCommand redisCommand = command.getRedisCommand();
            if (redisCommand.getCommandKeyType() == RedisCommand.CommandKeyType.None) {
                sendNoneKeyCommand(redisCommand, command, future);
            } else {
                List<byte[]> keys = command.getKeys();
                if (keys.size() == 1) {
                    byte[] key = keys.get(0);
                    sendCommand(key, command, future);
                } else {
                    sendMultiKeyCommand(redisCommand, command, future);
                }
            }
        }
    }

    @Override
    public void start() {
        this.executor = initExecutor();
        KVClient kvClient = initKVClient();
        this.commanders = initCommanders(kvClient);
    }

    private void sendNoneKeyCommand(RedisCommand redisCommand, Command command, CompletableFuture<Reply> future) {
        if (redisCommand == RedisCommand.PING) {
            future.complete(StatusReply.PONG);
        } else if (redisCommand == RedisCommand.ECHO) {
            byte[][] objects = command.getObjects();
            if (objects.length == 2) {
                future.complete(new BulkReply(objects[1]));
            } else {
                future.complete(ErrorReply.argNumWrong(redisCommand));
            }
        } else {
            future.complete(ErrorReply.NOT_SUPPORT);
        }
    }

    private void sendMultiKeyCommand(RedisCommand redisCommand, Command command, CompletableFuture<Reply> future) {
        List<byte[]> keys = command.getKeys();
        if (redisCommand == RedisCommand.DEL || redisCommand == RedisCommand.UNLINK || redisCommand == RedisCommand.EXISTS) {
            List<CompletableFuture<Reply>> futures = new ArrayList<>(keys.size());
            for (byte[] key : keys) {
                CompletableFuture<Reply> f = new CompletableFuture<>();
                sendCommand(key, new Command(new byte[][]{redisCommand.raw(), key}), f);
                futures.add(f);
            }
            CompletableFutureUtils.allOf(futures).thenAccept(replies -> future.complete(Utils.mergeIntegerReply(replies)));
        } else {
            future.complete(ErrorReply.NOT_SUPPORT);
        }
    }

    private void sendCommand(byte[] key, Command command, CompletableFuture<Reply> future) {
        try {
            executor.submit(key, () -> {
                try {
                    Reply reply = commanders.execute(command);
                    future.complete(reply);
                } catch (Exception e) {
                    future.complete(ErrorReply.NOT_AVAILABLE);
                }
            });
        } catch (Exception e) {
            future.complete(ErrorReply.TOO_BUSY);
        }
    }

    private CamelliaHashedExecutor initExecutor() {
        //init upstream redis connection
        RedisConnectionHub.getInstance().init(GlobalRedisProxyEnv.getTranspondProperties(), GlobalRedisProxyEnv.getProxyBeanFactory());

        //init upstream redis event loop
        CamelliaTranspondProperties.RedisConfProperties redisConf = GlobalRedisProxyEnv.getTranspondProperties().getRedisConf();
        NettyTransportMode nettyTransportMode = GlobalRedisProxyEnv.getNettyTransportMode();
        if (nettyTransportMode == NettyTransportMode.epoll) {
            this.eventLoopGroup = new EpollEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-kv-redis-connection"));
        } else if (nettyTransportMode == NettyTransportMode.kqueue) {
            this.eventLoopGroup = new KQueueEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-kv-redis-connection"));
        } else if (nettyTransportMode == NettyTransportMode.io_uring) {
            this.eventLoopGroup = new IOUringEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-kv-redis-connection"));
        } else {
            this.eventLoopGroup = new NioEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-kv-redis-connection"));
        }

        Runnable workThreadInitCallback = () -> RedisConnectionHub.getInstance().updateEventLoop(eventLoopGroup.next());

        int threads = ProxyDynamicConf.getInt("kv.command.executor.threads", SysUtils.getCpuNum() * 4);
        int queueSize = ProxyDynamicConf.getInt("kv.command.executor.queue.size", 100000);
        return new CamelliaHashedExecutor("kv-command-executor", threads, queueSize,
                new CamelliaHashedExecutor.AbortPolicy(), workThreadInitCallback);
    }

    private KVClient initKVClient() {
        String className = ProxyDynamicConf.getString("kv.client.class.name", null);
        return (KVClient) GlobalRedisProxyEnv.getProxyBeanFactory().getBean(BeanInitUtils.parseClass(className));
    }

    private Commanders initCommanders(KVClient kvClient) {
        KeyStruct keyStruct = new KeyStruct(namespace.getBytes(StandardCharsets.UTF_8));
        boolean valueCacheEnable = ProxyDynamicConf.getBoolean("kv.value.cache.enable", true);
        boolean metaCacheEnable = ProxyDynamicConf.getBoolean("kv.meta.cache.enable", true);
        CacheConfig cacheConfig = new CacheConfig(namespace, metaCacheEnable, valueCacheEnable);
        KvConfig kvConfig = new KvConfig(namespace);

        String metaCacheRedisUrl = ProxyDynamicConf.getString("kv.meta.cache.redis.url", null);
        RedisTemplate metaCacheRedisTemplate = new RedisTemplate(new UpstreamRedisClientTemplate(ReadableResourceTableUtil.parseTable(metaCacheRedisUrl)));
        KeyMetaServer keyMetaServer = new DefaultKeyMetaServer(kvClient, metaCacheRedisTemplate, keyStruct, cacheConfig);

        String cacheRedisUrl = ProxyDynamicConf.getString("kv.value.cache.redis.url", null);
        RedisTemplate valueCacheRedisTemplate = new RedisTemplate(new UpstreamRedisClientTemplate(ReadableResourceTableUtil.parseTable(cacheRedisUrl)));

        CommanderConfig commanderConfig = new CommanderConfig(kvClient, keyStruct, cacheConfig, kvConfig, keyMetaServer, valueCacheRedisTemplate);
        return new Commanders(commanderConfig);
    }

    @Override
    public void preheat() {
        //do nothing
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void shutdown() {
        //do nothing
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public void renew() {
        //do nothing
    }
}
