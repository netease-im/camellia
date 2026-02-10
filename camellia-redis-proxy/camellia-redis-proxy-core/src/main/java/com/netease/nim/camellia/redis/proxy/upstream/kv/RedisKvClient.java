package com.netease.nim.camellia.redis.proxy.upstream.kv;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.base.resource.RedisKvResource;
import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeConfig;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.conf.ServerConf;
import com.netease.nim.camellia.redis.proxy.enums.ProxyMode;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvRunToCompletionMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.redis.proxy.upstream.RedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.upstream.UpstreamRedisClientTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBuffer;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisHash;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.CacheConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KeyDesign;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KvConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.gc.KvGcExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.DecoratorKVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.DefaultKeyMetaServer;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMetaServer;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.SlotLock;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.SlotReadWriteLock;
import com.netease.nim.camellia.redis.proxy.upstream.utils.CompletableFutureUtils;
import com.netease.nim.camellia.redis.proxy.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by caojiajun on 2024/4/17
 */
public class RedisKvClient implements IUpstreamClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisKvClient.class);

    private MpscSlotHashExecutor executor;
    private ThreadPoolExecutor scanCommandExecutor;
    private final Resource resource;
    private final String namespace;
    private Commanders commanders;
    private boolean runToCompletionEnable;
    private final SlotReadWriteLock slotReadWriteLock = new SlotReadWriteLock();
    private final SlotLock slotLock = new SlotLock();
    private boolean commandExecutorAsyncEnable;

    public RedisKvClient(RedisKvResource resource) {
        this.resource = resource;
        this.namespace = resource.getNamespace();
        checkConfig();
    }

    private void checkConfig() {
        boolean standaloneModeEnable = ProxyDynamicConf.getBoolean("kv.standalone.mode.enable", false);
        if (!standaloneModeEnable) {
            boolean clusterModeEnable = ServerConf.proxyMode() == ProxyMode.cluster;
            if (!clusterModeEnable) {
                logger.error("proxy cluster mode not enabled, kv client possible concurrency issues");
                throw new KvException("proxy cluster mode not enabled, kv client possible concurrency issues");
            } else {
                if (!ClusterModeConfig.clusterModeCommandMoveAlways()) {
                    logger.error("proxy cluster mode command move always not enabled, kv client possible concurrency issues");
                    throw new KvException("proxy cluster mode command move always not enabled, kv client possible concurrency issues");
                }
            }
        }
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
                    byte[] key = keys.getFirst();
                    sendCommand(redisCommand, key, command, future);
                } else {
                    sendMultiKeyCommand(redisCommand, command, future);
                }
            }
        }
    }

    @Override
    public void start() {
        this.executor = KvExecutors.getInstance().getCommandExecutor();
        this.scanCommandExecutor = KvExecutors.getInstance().getScanCommandExecutor();
        initConf();
        this.commanders = initCommanders();
        logger.info("RedisKvClient start success, resource = {}", getResource());
    }


    private void sendCommand(RedisCommand redisCommand, byte[] key, Command command, CompletableFuture<Reply> future) {
        try {
            int slot = RedisClusterCRC16Utils.getSlot(key);
            Commander commander = commanders.getCommander(redisCommand);
            if (commander == null) {
                future.complete(Utils.commandNotSupport(redisCommand));
                return;
            }
            if (!commanders.parse(commander, command)) {
                future.complete(ErrorReply.argNumWrong(redisCommand));
                return;
            }

            if (!commandExecutorAsyncEnable) {
                try {
                    ReentrantLock lock = slotLock.getLock(slot);
                    lock.lock();
                    Reply reply;
                    try {
                        reply = commanders.execute(commander, slot, command);
                    } finally {
                        lock.unlock();
                    }
                    if (reply == null) {
                        ErrorLogCollector.collect(RedisKvClient.class, "command receive null reply, command = " + command.getName());
                        reply = ErrorReply.NOT_AVAILABLE;
                    }
                    future.complete(reply);
                } catch (Exception e) {
                    ErrorLogCollector.collect(RedisKvClient.class, "send command error, command = " + command.getName(), e);
                    future.complete(ErrorReply.NOT_AVAILABLE);
                }
                return;
            }

            ReentrantReadWriteLock lock;
            if (runToCompletionEnable) {
                lock = slotReadWriteLock.getLock(slot);
                if (redisCommand.getType() == RedisCommand.Type.READ) {
                    ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
                    if (executor.canRunToCompletion(slot) && readLock.tryLock()) {
                        Reply reply;
                        try {
                            reply = commanders.runToCompletion(commander, slot, command);
                        } catch (Exception e) {
                            ErrorLogCollector.collect(RedisKvClient.class, "send command run to completion error, command = " + command.getName(), e);
                            reply = ErrorReply.INTERNAL_ERROR;
                        } finally {
                            readLock.unlock();
                        }
                        if (reply != null) {
                            KvRunToCompletionMonitor.update(namespace, command.getName(), true);
                            future.complete(reply);
                            return;
                        }
                    }
                    KvRunToCompletionMonitor.update(namespace, command.getName(), false);
                }
            } else {
                lock = null;
            }
            executor.submit(slot, () -> {
                try {
                    ReentrantReadWriteLock.WriteLock writeLock = null;
                    if (lock != null) {
                        writeLock = lock.writeLock();
                        writeLock.lock();
                    }
                    Reply reply;
                    try {
                        reply = commanders.execute(commander, slot, command);
                    } finally {
                        if (writeLock != null) {
                            writeLock.unlock();
                        }
                    }
                    if (reply == null) {
                        ErrorLogCollector.collect(RedisKvClient.class, "command receive null reply, command = " + command.getName());
                    }
                    future.complete(reply);
                } catch (Exception e) {
                    ErrorLogCollector.collect(RedisKvClient.class, "send command error, command = " + command.getName(), e);
                    future.complete(ErrorReply.NOT_AVAILABLE);
                }
            });
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisKvClient.class, "send command error, command = " + command.getName(), e);
            future.complete(ErrorReply.TOO_BUSY);
        }
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
        } else if (redisCommand == RedisCommand.SCAN) {
            sendScanCommand(command, future);
        } else {
            future.complete(Utils.commandNotSupport(redisCommand));
        }
    }

    private void sendScanCommand(Command command, CompletableFuture<Reply> future) {
        try {
            scanCommandExecutor.submit(() -> {
                try {
                    Commander commander = commanders.getCommander(RedisCommand.SCAN);
                    if (!commanders.parse(commander, command)) {
                        future.complete(ErrorReply.argNumWrong(RedisCommand.SCAN));
                        return;
                    }
                    Reply reply = commanders.execute(commander, -1, command);
                    if (reply == null) {
                        ErrorLogCollector.collect(RedisKvClient.class, "command receive null reply, command = " + command.getName());
                    }
                    future.complete(reply);
                } catch (Exception e) {
                    ErrorLogCollector.collect(RedisKvClient.class, "send command error, command = " + command.getName(), e);
                    future.complete(ErrorReply.NOT_AVAILABLE);
                }
            });
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisKvClient.class, "send command error, command = " + command.getName(), e);
            future.complete(ErrorReply.TOO_BUSY);
        }
    }

    private void sendMultiKeyCommand(RedisCommand redisCommand, Command command, CompletableFuture<Reply> future) {
        List<byte[]> keys = command.getKeys();
        if (redisCommand == RedisCommand.DEL || redisCommand == RedisCommand.UNLINK || redisCommand == RedisCommand.EXISTS) {
            List<CompletableFuture<Reply>> futures = new ArrayList<>(keys.size());
            for (byte[] key : keys) {
                CompletableFuture<Reply> f = new CompletableFuture<>();
                sendCommand(redisCommand, key, new Command(new byte[][]{redisCommand.raw(), key}), f);
                futures.add(f);
            }
            CompletableFutureUtils.allOf(futures).thenAccept(replies -> future.complete(Utils.mergeIntegerReply(replies)));
        } else if (redisCommand == RedisCommand.MGET) {
            List<CompletableFuture<Reply>> futures = new ArrayList<>(keys.size());
            for (byte[] key : keys) {
                CompletableFuture<Reply> f = new CompletableFuture<>();
                sendCommand(RedisCommand.GET, key, new Command(new byte[][]{RedisCommand.GET.raw(), key}), f);
                futures.add(f);
            }
            CompletableFutureUtils.allOf(futures).thenAccept(replies -> {
                for (Reply reply : replies) {
                    if (reply instanceof ErrorReply) {
                        future.complete(reply);
                        return;
                    }
                }
                future.complete(new MultiBulkReply(replies.toArray(new Reply[0])));
            });
        } else if (redisCommand == RedisCommand.MSET) {
            byte[][] objects = command.getObjects();
            if (objects.length < 3 || objects.length % 2 == 0) {
                future.complete(ErrorReply.argNumWrong(redisCommand));
                return;
            }
            List<CompletableFuture<Reply>> futures = new ArrayList<>(keys.size());
            for (int i=1; i<objects.length; i+=2) {
                byte[] key = objects[i];
                byte[] value = objects[i+1];
                CompletableFuture<Reply> f = new CompletableFuture<>();
                sendCommand(RedisCommand.SET, key, new Command(new byte[][]{RedisCommand.SET.raw(), key, value}), f);
                futures.add(f);
            }
            CompletableFutureUtils.allOf(futures).thenAccept(replies -> {
                for (Reply reply : replies) {
                    if (reply instanceof ErrorReply) {
                        future.complete(reply);
                        return;
                    }
                }
                future.complete(StatusReply.OK);
            });
        } else {
            future.complete(Utils.commandNotSupport(redisCommand));
        }
    }

    private KVClient initKVClient() {
        String className = RedisKvConf.getClassName(namespace, "kv.client", null);
        return (KVClient) ServerConf.getProxyBeanFactory().getBean(BeanInitUtils.parseClass(className));
    }

    private Commanders initCommanders() {
        KVClient kvClient = initKVClient();
        try {
            kvClient.init(namespace);
            //get for check and warm
            kvClient.get(0, UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        } catch (Throwable e) {
            logger.error("kv client get error, namespace = {}", namespace, e);
            throw new KvException(e);
        }
        kvClient = new DecoratorKVClient(namespace, kvClient);

        KeyDesign keyDesign = new KeyDesign(namespace);
        KvConfig kvConfig = new KvConfig(namespace);

        KvGcExecutor gcExecutor = new KvGcExecutor(kvClient, keyDesign, kvConfig);
        gcExecutor.start();

        if (keyDesign.zsetEncodeVersion() == EncodeVersion.version_1 && !commandExecutorAsyncEnable) {
            logger.warn("zset encode version 1 should config 'kv.command.executor.async.enable' = true");
            commandExecutorAsyncEnable = true;
        }

        CacheConfig cacheConfig = new CacheConfig(namespace);

        RedisTemplate cacheRedisTemplate = initRedisTemplate("kv.redis.cache");
        RedisTemplate storageRedisTemplate = initRedisTemplate("kv.redis.store");

        KeyMetaServer keyMetaServer = new DefaultKeyMetaServer(kvClient, keyDesign, gcExecutor, cacheConfig);

        WriteBuffer<RedisHash> hashWriteBuffer = WriteBuffer.newWriteBuffer(namespace, "hash");
        WriteBuffer<RedisZSet> zsetWriteBuffer = WriteBuffer.newWriteBuffer(namespace, "zset");
        WriteBuffer<RedisSet> setWriteBuffer = WriteBuffer.newWriteBuffer(namespace, "set");

        CommanderConfig commanderConfig = new CommanderConfig(kvClient, keyDesign, cacheConfig, kvConfig,
                keyMetaServer, cacheRedisTemplate, storageRedisTemplate, gcExecutor, hashWriteBuffer, zsetWriteBuffer, setWriteBuffer);

        return new Commanders(commanderConfig);
    }

    private RedisTemplate initRedisTemplate(String key) {
        String type = RedisKvConf.getString(namespace, key + ".config.type", "local");
        if (type.equalsIgnoreCase("local")) {
            String url = RedisKvConf.getString(namespace, key + ".url", null);
            if (url == null) {
                return null;
            }
            ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(url);
            RedisProxyEnv env = GlobalRedisProxyEnv.getRedisProxyEnv();
            UpstreamRedisClientTemplate template = new UpstreamRedisClientTemplate(env, resourceTable);
            RedisTemplate redisTemplate = new RedisTemplate(template);
            logger.info("redis template init success, namespace = {}, key = {}, resource = {}",
                    namespace, key, ReadableResourceTableUtil.readableResourceTable(PasswordMaskUtils.maskResourceTable(resourceTable)));
            return redisTemplate;
        } else if (type.equalsIgnoreCase("remote")) {
            String dashboardUrl = RedisKvConf.getString(namespace, key + ".camellia.dashboard.url", null);
            if (dashboardUrl == null) {
                throw new KvException("illegal dashboardUrl");
            }
            boolean monitorEnable = RedisKvConf.getBoolean(namespace, key + ".camellia.dashboard.monitor.enable", true);
            long checkIntervalMillis = RedisKvConf.getLong(namespace, key + ".camellia.dashboard.check.interval.millis", 3000L);
            long bid = RedisKvConf.getLong(namespace, key + ".bid", -1);
            String bgroup = RedisKvConf.getString(namespace, key + ".bgroup", "default");
            if (bid <= 0) {
                throw new KvException("illegal bid");
            }
            RedisProxyEnv env = GlobalRedisProxyEnv.getRedisProxyEnv();
            CamelliaApi camelliaApi = CamelliaApiUtil.init(dashboardUrl);
            UpstreamRedisClientTemplate template = new UpstreamRedisClientTemplate(env, camelliaApi, bid, bgroup, monitorEnable, checkIntervalMillis);
            RedisTemplate redisTemplate = new RedisTemplate(template);
            logger.info("redis template init success, namespace = {}, key = {}, dashboardUrl = {}, bid = {}, bgroup = {}",
                    namespace, key, dashboardUrl, bid, bgroup);
            return redisTemplate;
        } else {
            throw new KvException("init redis template error");
        }
    }

    private void initConf() {
        final String confKey = "kv.run.to.completion.enable";
        this.runToCompletionEnable = RedisKvConf.getBoolean(namespace, confKey, true);
        logger.info("namespace = {}, runToCompletionEnable = {}", namespace, runToCompletionEnable);
        ProxyDynamicConf.registerCallback(() -> {
            boolean runToCompletionEnable = RedisKvConf.getBoolean(namespace, confKey, true);
            if (RedisKvClient.this.runToCompletionEnable != runToCompletionEnable) {
                RedisKvClient.this.runToCompletionEnable = runToCompletionEnable;
                logger.info("namespace = {}, runToCompletionEnable = {}", namespace, runToCompletionEnable);
            }
        });
        this.commandExecutorAsyncEnable = ProxyDynamicConf.getBoolean("kv.command.executor.async.enable", false);
        logger.info("namespace = {}, commandExecutorAsyncEnable = {}", namespace, commandExecutorAsyncEnable);
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
