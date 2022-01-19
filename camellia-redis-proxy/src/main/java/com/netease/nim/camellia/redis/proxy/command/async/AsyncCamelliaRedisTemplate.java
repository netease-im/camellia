package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.api.*;
import com.netease.nim.camellia.core.client.env.Monitor;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceChooser;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.route.ProxyRouteConfUpdater;
import com.netease.nim.camellia.redis.proxy.conf.MultiWriteMode;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.monitor.*;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.*;
import com.netease.nim.camellia.redis.resource.RedisResourceUtil;
import com.netease.nim.camellia.redis.resource.RedisType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2019/12/19.
 */
public class AsyncCamelliaRedisTemplate implements IAsyncCamelliaRedisTemplate {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCamelliaRedisTemplate.class);

    private static final ScheduledExecutorService scheduleExecutor
            = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(ReloadTask.class));

    private static final long defaultBid = -1;
    private static final String defaultBgroup = "local";
    private static final long defaultCheckIntervalMillis = 5000;
    private static final boolean defaultMonitorEnable = false;

    private final AsyncNettyClientFactory factory;

    private final long bid;
    private final String bgroup;
    private AsyncCamelliaRedisEnv env;
    private Monitor monitor;
    private ResourceChooser resourceChooser;

    private AsyncClient singletonClient;
    private boolean isSingletonStandaloneRedisOrRedisSentinel;
    private boolean isSingletonStandaloneRedisOrRedisSentinelOrRedisCluster;

    private final MultiWriteMode multiWriteMode;

    private ScanCursorCalculator cursorCalculator;

    private final ResourceTableUpdateCallback callback = resourceTable -> {
        RedisResourceUtil.checkResourceTable(resourceTable);
        init(resourceTable);
    };

    public AsyncCamelliaRedisTemplate(ResourceTable resourceTable) {
        this(AsyncCamelliaRedisEnv.defaultRedisEnv(), resourceTable);
    }

    public AsyncCamelliaRedisTemplate(AsyncCamelliaRedisEnv env, ResourceTable resourceTable) {
        this(env, new LocalCamelliaApi(resourceTable), defaultBid, defaultBgroup, defaultMonitorEnable, defaultCheckIntervalMillis, false);
    }

    public AsyncCamelliaRedisTemplate(AsyncCamelliaRedisEnv env, String resourceTableFilePath, long checkIntervalMillis) {
        this(env, new ReloadableLocalFileCamelliaApi(resourceTableFilePath, RedisResourceUtil.RedisResourceTableChecker),
                defaultBid, defaultBgroup, defaultMonitorEnable, checkIntervalMillis, true);
    }

    public AsyncCamelliaRedisTemplate(AsyncCamelliaRedisEnv env, CamelliaApi service, long bid, String bgroup,
                                      boolean monitorEnable, long checkIntervalMillis) {
        this(env, service, bid, bgroup, monitorEnable, checkIntervalMillis, true);
    }

    public AsyncCamelliaRedisTemplate(AsyncCamelliaRedisEnv env, CamelliaApi service, long bid, String bgroup,
                                      boolean monitorEnable, long checkIntervalMillis, boolean reload) {
        this.env = env;
        this.bid = bid;
        this.bgroup = bgroup;
        this.factory = env.getClientFactory();
        this.multiWriteMode = env.getMultiWriteMode();
        CamelliaApiResponse response = service.getResourceTable(bid, bgroup, null);
        String md5 = response.getMd5();
        if (response.getResourceTable() == null) {
            throw new CamelliaRedisException("resourceTable is null");
        }
        RedisResourceUtil.checkResourceTable(response.getResourceTable());
        this.init(response.getResourceTable());
        if (logger.isInfoEnabled()) {
            logger.info("AsyncCamelliaRedisTemplate init success, bid = {}, bgroup = {}, md5 = {}, resourceTable = {}", bid, bgroup, md5,
                    ReadableResourceTableUtil.readableResourceTable(PasswordMaskUtils.maskResourceTable(response.getResourceTable())));
        }
        if (reload) {
            ReloadTask reloadTask = new ReloadTask(this, service, bid, bgroup, md5);
            scheduleExecutor.scheduleAtFixedRate(reloadTask, checkIntervalMillis, checkIntervalMillis, TimeUnit.MILLISECONDS);
            if (monitorEnable) {
                Monitor monitor = new FastRemoteMonitor(bid, bgroup, service);
                ProxyEnv proxyEnv = new ProxyEnv.Builder(env.getProxyEnv()).monitor(monitor).build();
                this.env = new AsyncCamelliaRedisEnv.Builder(env).proxyEnv(proxyEnv).build();
                this.monitor = monitor;
            }
        }
        if (bid == -1) {
            RouteConfMonitor.registerRedisTemplate(null, null, this);
        } else {
            RouteConfMonitor.registerRedisTemplate(bid, bgroup, this);
        }
    }

    public AsyncCamelliaRedisTemplate(AsyncCamelliaRedisEnv env, long bid, String bgroup, ProxyRouteConfUpdater updater, long reloadIntervalMillis) {
        this.env = env;
        this.bid = bid;
        this.bgroup = bgroup;
        this.factory = env.getClientFactory();
        this.multiWriteMode = env.getMultiWriteMode();
        ResourceTable resourceTable = updater.getResourceTable(bid, bgroup);
        RedisResourceUtil.checkResourceTable(resourceTable);
        this.init(resourceTable);
        if (logger.isInfoEnabled()) {
            logger.info("AsyncCamelliaRedisTemplate init success, bid = {}, bgroup = {}, resourceTable = {}, ProxyRouteConfUpdater = {}", bid, bgroup,
                    ReadableResourceTableUtil.readableResourceTable(PasswordMaskUtils.maskResourceTable(resourceTable)), updater.getClass().getName());
        }
        if (reloadIntervalMillis > 0) {
            ProxyRouteConfUpdaterReloadTask reloadTask = new ProxyRouteConfUpdaterReloadTask(this, resourceTable, bid, bgroup, updater);
            scheduleExecutor.scheduleAtFixedRate(reloadTask, reloadIntervalMillis, reloadIntervalMillis, TimeUnit.MILLISECONDS);
        }
        if (bid == -1) {
            RouteConfMonitor.registerRedisTemplate(null, null, this);
        } else {
            RouteConfMonitor.registerRedisTemplate(bid, bgroup, this);
        }
    }

    public ResourceTable getResourceTable() {
        return resourceChooser.getResourceTable();
    }

    public long getResourceTableUpdateTime() {
        return resourceChooser.getCreateTime();
    }

    public ResourceTableUpdateCallback getCallback() {
        return callback;
    }

    public void preheat() {
        Set<Resource> allResources = this.resourceChooser.getAllResources();
        for (Resource resource : allResources) {
            AsyncClient client = factory.get(resource.getUrl());
            client.preheat();
        }
    }

    private boolean isPassThroughCommand(List<Command> commands) {
        if (!isSingletonStandaloneRedisOrRedisSentinelOrRedisCluster) return false;
        for (Command command : commands) {
            RedisCommand redisCommand = command.getRedisCommand();
            if (redisCommand.getSupportType() == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_1
                    || redisCommand.getSupportType() == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_2) {
                return false;
            }
        }
        return true;
    }

    public List<CompletableFuture<Reply>> sendCommand(List<Command> commands) {
        List<CompletableFuture<Reply>> futureList = new ArrayList<>(commands.size());

        if (isPassThroughCommand(commands)) {
            String url = getReadResource(Utils.EMPTY_ARRAY).getUrl();
            for (Command command : commands) {
                CompletableFuture<Reply> future = new CompletableFuture<>();
                RedisCommand redisCommand = command.getRedisCommand();
                RedisCommand.Type type = redisCommand.getType();
                if (type == RedisCommand.Type.READ) {
                    incrRead(url, command);
                } else if (type == RedisCommand.Type.WRITE) {
                    incrWrite(url, command);
                }
                futureList.add(future);
            }
            if (singletonClient != null) {
                singletonClient.sendCommand(commands, futureList);
            } else {
                AsyncClient client = factory.get(url);
                client.sendCommand(commands, futureList);
            }
            return futureList;
        }

        CommandFlusher commandFlusher = new CommandFlusher(commands.size());
        for (Command command : commands) {
            RedisCommand redisCommand = command.getRedisCommand();

            if (redisCommand.getSupportType() == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_1) {
                if (!isSingletonStandaloneRedisOrRedisSentinelOrRedisCluster) {
                    CompletableFuture<Reply> future = new CompletableFuture<>();
                    future.complete(ErrorReply.NOT_SUPPORT);
                    futureList.add(future);
                    continue;
                }
                Resource resource = resourceChooser.getReadResource(Utils.EMPTY_ARRAY);
                AsyncClient client = factory.get(resource.getUrl());
                CompletableFuture<Reply> future;
                switch (redisCommand) {
                    case PUBSUB:
                    case PUBLISH:
                    case SUBSCRIBE:
                    case PSUBSCRIBE:
                    case UNSUBSCRIBE:
                    case PUNSUBSCRIBE:
                        future = commandFlusher.sendCommand(client, command);
                        incrWrite(resource, command);
                        break;
                    default:
                        future = new CompletableFuture<>();
                        future.complete(ErrorReply.NOT_SUPPORT);
                        futureList.add(future);
                        break;
                }
                futureList.add(future);
                continue;
            }

            if (redisCommand.getSupportType() == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_2) {
                if (!isSingletonStandaloneRedisOrRedisSentinel) {
                    CompletableFuture<Reply> future = new CompletableFuture<>();
                    future.complete(ErrorReply.NOT_SUPPORT);
                    futureList.add(future);
                    continue;
                }
                CompletableFuture<Reply> future;
                switch (redisCommand) {
                    case KEYS:
                    case WATCH:
                    case UNWATCH:
                    case MULTI:
                    case EXEC:
                    case RANDOMKEY:
                    case DISCARD:
                        Resource resource = resourceChooser.getReadResource(Utils.EMPTY_ARRAY);
                        String url = resource.getUrl();
                        AsyncClient client = factory.get(url);
                        future = commandFlusher.sendCommand(client, command);
                        incrRead(resource, command);
                        break;
                    default:
                        future = new CompletableFuture<>();
                        future.complete(ErrorReply.NOT_SUPPORT);
                        break;
                }
                futureList.add(future);
                continue;
            }

            if (redisCommand.getSupportType() == RedisCommand.CommandSupportType.RESTRICTIVE_SUPPORT) {
                CompletableFuture<Reply> future;
                switch (redisCommand) {
                    case EVAL:
                    case EVALSHA:
                        future = doEvalOrEvalSha(command, commandFlusher, false);
                        break;
                    case EVAL_RO:
                    case EVALSHA_RO:
                        future = doEvalOrEvalSha(command, commandFlusher, true);
                        break;
                    case PFCOUNT:
                    case SDIFF:
                    case SINTER:
                    case SUNION:
                        if (command.getObjects().length < 2) {
                            future = new CompletableFuture<>();
                            future.complete(ErrorReply.argNumWrong(redisCommand));
                        } else {
                            future = readCommandWithDynamicKeyCount(command, commandFlusher, 1, command.getObjects().length - 1);
                        }
                        break;
                    case PFMERGE:
                    case SINTERSTORE:
                    case SUNIONSTORE:
                    case SDIFFSTORE:
                    case RPOPLPUSH:
                        if (command.getObjects().length < 2) {
                            future = new CompletableFuture<>();
                            future.complete(ErrorReply.argNumWrong(redisCommand));
                        } else {
                            future = writeCommandWithDynamicKeyCount(command, commandFlusher, 1, command.getObjects().length - 1);
                        }
                        break;
                    case RENAME:
                    case RENAMENX:
                    case SMOVE:
                    case LMOVE:
                    case GEOSEARCHSTORE:
                    case ZRANGESTORE:
                    case BLMOVE:
                        if (command.getObjects().length < 3) {
                            future = new CompletableFuture<>();
                            future.complete(ErrorReply.argNumWrong(redisCommand));
                        } else {
                            future = writeCommandWithDynamicKeyCount(command, commandFlusher, 1, 2);
                        }
                        break;
                    case ZINTERSTORE:
                    case ZUNIONSTORE:
                    case ZDIFFSTORE:
                        future = zinterstoreOrZunionstore(command, commandFlusher);
                        break;
                    case BITOP:
                        future = bitop(command, commandFlusher);
                        break;
                    case MSETNX:
                        future = msetnx(command, commandFlusher);
                        break;
                    case BLPOP:
                    case BRPOP:
                    case BRPOPLPUSH:
                    case BZPOPMAX:
                    case BZPOPMIN:
                        future = writeCommandWithDynamicKeyCount(command, commandFlusher, 1, command.getObjects().length - 2);
                        break;
                    case XREAD:
                    case XREADGROUP:
                        future = xreadOrXreadgroup(command, commandFlusher);
                        break;
                    case ZDIFF:
                    case ZUNION:
                    case ZINTER:
                        if (command.getObjects().length < 3) {
                            future = new CompletableFuture<>();
                            future.complete(ErrorReply.argNumWrong(redisCommand));
                        } else {
                            int keyCount = (int) Utils.bytesToNum(command.getObjects()[1]);
                            future = writeCommandWithDynamicKeyCount(command, commandFlusher, 2, 1 + keyCount);
                        }
                        break;
                    default:
                        future = new CompletableFuture<>();
                        future.complete(ErrorReply.NOT_SUPPORT);
                        break;
                }
                futureList.add(future);
                continue;
            }

            if (redisCommand == RedisCommand.SCAN) {
                try {
                    if (isSingletonStandaloneRedisOrRedisSentinelOrRedisCluster) {
                        Resource resource = resourceChooser.getReadResource(Utils.EMPTY_ARRAY);
                        CompletableFuture<Reply> future = doRead(resource, commandFlusher, command);
                        futureList.add(future);
                    } else {
                        List<Resource> allReadResources = resourceChooser.getAllReadResources();
                        if (cursorCalculator == null || cursorCalculator.getNodeBitLen() != ScanCursorCalculator.getSuitableNodeBitLen(allReadResources.size())) {
                            cursorCalculator = new ScanCursorCalculator(ScanCursorCalculator.getSuitableNodeBitLen(allReadResources.size()));
                        }
                        byte[][] objects = command.getObjects();
                        if (objects == null || objects.length <= 1) {
                            CompletableFuture<Reply> future = new CompletableFuture<>();
                            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
                            continue;
                        }
                        int currentNodeIndex = cursorCalculator.filterScanCommand(command);
                        if (currentNodeIndex < 0) {
                            CompletableFuture<Reply> future = new CompletableFuture<>();
                            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
                            continue;
                        }
                        if (currentNodeIndex >= allReadResources.size()) {
                            CompletableFuture<Reply> future = new CompletableFuture<>();
                            future.complete(new ErrorReply("ERR illegal arguments of cursor"));
                            continue;
                        }
                        Resource resource = allReadResources.get(currentNodeIndex);
                        AsyncClient client = factory.get(resource.getUrl());
                        CompletableFuture<Reply> f = commandFlusher.sendCommand(client, command);
                        CompletableFuture<Reply> future = new CompletableFuture<>();
                        f.thenApply((reply) -> cursorCalculator.filterScanReply(reply, currentNodeIndex, allReadResources.size())).thenAccept(future::complete);
                        futureList.add(future);
                        incrRead(resource, command);
                    }
                } catch (Exception e) {
                    CompletableFuture<Reply> future = new CompletableFuture<>();
                    future.complete(ErrorReply.NOT_AVAILABLE);
                    futureList.add(future);
                }
                continue;
            }

            if (redisCommand == RedisCommand.SCRIPT) {
                byte[][] objects = command.getObjects();
                if (objects.length <= 1) {
                    CompletableFuture<Reply> future = new CompletableFuture<>();
                    future.complete(ErrorReply.argNumWrong(redisCommand));
                    futureList.add(future);
                    continue;
                }
                String ope = Utils.bytesToString(objects[1]);
                if (ope.equalsIgnoreCase(RedisKeyword.FLUSH.name())
                        || ope.equalsIgnoreCase(RedisKeyword.LOAD.name())) {
                    List<Resource> resources = resourceChooser.getAllWriteResources();
                    CompletableFuture<Reply> future = doWrite(resources, commandFlusher, command);
                    futureList.add(future);
                } else if (ope.equalsIgnoreCase(RedisKeyword.EXISTS.name())) {
                    List<Resource> resources = resourceChooser.getAllReadResources();
                    List<CompletableFuture<Reply>> futures = new ArrayList<>();
                    for (Resource resource : resources) {
                        CompletableFuture<Reply> future = doRead(resource, commandFlusher, command);
                        futures.add(future);
                    }
                    CompletableFuture<Reply> future = new CompletableFuture<>();
                    AsyncUtils.allOf(futures).thenAccept(replies -> future.complete(Utils.mergeMultiIntegerReply(replies)));
                    futureList.add(future);
                } else {
                    CompletableFuture<Reply> future = new CompletableFuture<>();
                    future.complete(ErrorReply.NOT_SUPPORT);
                    futureList.add(future);
                }
                continue;
            }

            //特殊处理多key的命令
            if (resourceChooser.getType() == ResourceTable.Type.SHADING) {
                boolean continueOk = false;
                switch (redisCommand) {
                    case MGET: {
                        if (command.getObjects().length > 2) {
                            CompletableFuture<Reply> future = mget(command, commandFlusher);
                            futureList.add(future);
                            continueOk = true;
                        }
                        break;
                    }
                    case TOUCH:
                    case UNLINK:
                    case DEL: {
                        if (command.getObjects().length > 2) {
                            CompletableFuture<Reply> future = delOrUnlinkOrTouch(command, commandFlusher);
                            futureList.add(future);
                            continueOk = true;
                        }
                        break;
                    }
                    case MSET: {
                        if (command.getObjects().length > 3) {
                            CompletableFuture<Reply> future = mset(command, commandFlusher);
                            futureList.add(future);
                            continueOk = true;
                        }
                        break;
                    }
                    case EXISTS: {
                        if (command.getObjects().length > 2) {
                            CompletableFuture<Reply> future = exists(command, commandFlusher);
                            futureList.add(future);
                            continueOk = true;
                        }
                        break;
                    }
                }
                if (continueOk) continue;
            }

            if (redisCommand.getCommandKeyType() != RedisCommand.CommandKeyType.SIMPLE_SINGLE) {
                CompletableFuture<Reply> completableFuture1 = null;
                switch (redisCommand) {
                    case XGROUP:
                        completableFuture1 = xgroup(command, commandFlusher);
                        break;
                    case XINFO:
                        completableFuture1 = xinfo(command, commandFlusher);
                        break;
                }
                if (completableFuture1 != null) {
                    futureList.add(completableFuture1);
                    continue;
                }
            }

            RedisCommand.Type type = redisCommand.getType();
            if (type == RedisCommand.Type.READ) {
                Resource resource = getReadResource(command);
                CompletableFuture<Reply> future = doRead(resource, commandFlusher, command);
                futureList.add(future);
            } else if (type == RedisCommand.Type.WRITE) {
                List<Resource> writeResources = getWriteResources(command);
                CompletableFuture<Reply> future = doWrite(writeResources, commandFlusher, command);
                futureList.add(future);
            } else {
                throw new CamelliaRedisException("not support RedisCommand.Type");
            }
        }
        commandFlusher.flush();
        return futureList;
    }

    private CompletableFuture<Reply> xreadOrXreadgroup(Command command, CommandFlusher commandFlusher) {
        byte[][] objects = command.getObjects();
        int index = -1;
        for (int i = 1; i < objects.length; i++) {
            String string = new String(objects[i], Utils.utf8Charset);
            if (string.equalsIgnoreCase(RedisKeyword.STREAMS.name())) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            CompletableFuture<Reply> future = new CompletableFuture<>();
            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
            return future;
        }
        int last = objects.length - index - 1;
        if (last <= 0) {
            CompletableFuture<Reply> future = new CompletableFuture<>();
            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
            return future;
        }
        if (last % 2 != 0) {
            CompletableFuture<Reply> future = new CompletableFuture<>();
            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
            return future;
        }
        int keyCount = last / 2;
        return writeCommandWithDynamicKeyCount(command, commandFlusher, index + 1, index + keyCount);
    }

    private CompletableFuture<Reply> xgroup(Command command, CommandFlusher commandFlusher) {
        byte[][] objects = command.getObjects();
        if (objects.length < 3) {
            CompletableFuture<Reply> future = new CompletableFuture<>();
            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
            return future;
        }
        byte[] key = objects[2];
        List<Resource> writeResources = resourceChooser.getWriteResources(key);
        return doWrite(writeResources, commandFlusher, command);
    }

    private CompletableFuture<Reply> xinfo(Command command, CommandFlusher commandFlusher) {
        byte[][] objects = command.getObjects();
        if (objects.length < 3) {
            CompletableFuture<Reply> future = new CompletableFuture<>();
            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
            return future;
        }
        byte[] key = objects[2];
        Resource resource = resourceChooser.getReadResource(key);
        return doRead(resource, commandFlusher, command);
    }

    private CompletableFuture<Reply> doEvalOrEvalSha(Command command, CommandFlusher commandFlusher, boolean readonly) {
        RedisCommand redisCommand = command.getRedisCommand();
        byte[][] objects = command.getObjects();
        if (objects.length <= 2) {
            CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
            completableFuture.complete(ErrorReply.argNumWrong(redisCommand));
            return completableFuture;
        }
        if (resourceChooser.getType() == ResourceTable.Type.SHADING) {
            return evalOrEvalSha(redisCommand, command, commandFlusher, readonly);
        } else {
            if (readonly) {
                Resource resource = resourceChooser.getReadResource(Utils.EMPTY_ARRAY);
                return doRead(resource, commandFlusher, command);
            } else {
                List<Resource> writeResources = resourceChooser.getWriteResources(Utils.EMPTY_ARRAY);
                return doWrite(writeResources, commandFlusher, command);
            }
        }
    }

    private CompletableFuture<Reply> doRead(Resource resource, CommandFlusher commandFlusher, Command command) {
        AsyncClient client = factory.get(resource.getUrl());
        CompletableFuture<Reply> future = commandFlusher.sendCommand(client, command);
        incrRead(resource, command);
        return future;
    }

    private CompletableFuture<Reply> doWrite(List<Resource> writeResources, CommandFlusher commandFlusher, Command command) {
        List<CompletableFuture<Reply>> list = new ArrayList<>(writeResources.size());
        for (Resource resource : writeResources) {
            AsyncClient client = factory.get(resource.getUrl());
            CompletableFuture<Reply> future = commandFlusher.sendCommand(client, command);
            incrWrite(resource, command);
            list.add(future);
        }
        return AsyncUtils.finalReply(list, multiWriteMode);
    }

    private CompletableFuture<Reply> evalOrEvalSha(RedisCommand redisCommand, Command command, CommandFlusher commandFlusher, boolean readOnly) {
        byte[][] objects = command.getObjects();
        long keyCount = Utils.bytesToNum(objects[2]);
        if (keyCount == 0) {
            if (readOnly) {
                Resource resource = resourceChooser.getReadResource(Utils.EMPTY_ARRAY);
                return doRead(resource, commandFlusher, command);
            } else {
                List<Resource> writeResources = resourceChooser.getWriteResources(Utils.EMPTY_ARRAY);
                return doWrite(writeResources, commandFlusher, command);
            }
        } else if (keyCount == 1) {
            if (objects.length < 4) {
                CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
                completableFuture.complete(ErrorReply.argNumWrong(redisCommand));
                return completableFuture;
            }
            byte[] key = objects[3];
            if (readOnly) {
                Resource resource = resourceChooser.getReadResource(key);
                return doRead(resource, commandFlusher, command);
            } else {
                List<Resource> writeResources = resourceChooser.getWriteResources(key);
                return doWrite(writeResources, commandFlusher, command);
            }
        } else if (keyCount < 0) {
            CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
            completableFuture.complete(new ErrorReply("ERR Number of keys can't be negative"));
            return completableFuture;
        } else {
            //判断一下是否所有key分片计算后是指向的同一组resource
            if (objects.length < 3 + keyCount) {
                CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
                completableFuture.complete(ErrorReply.argNumWrong(redisCommand));
                return completableFuture;
            }
            if (readOnly) {
                ResourceChooser.ReadResourceBean resources = resourceChooser.getReadResources(objects[3]);
                for (int i = 4; i < 3 + keyCount; i++) {
                    byte[] key = objects[i];
                    ResourceChooser.ReadResourceBean readResources = resourceChooser.getReadResources(key);
                    boolean checkReadResourcesEqual = ResourceChooser.checkReadResourcesEqual(resources, readResources);
                    if (!checkReadResourcesEqual) {
                        CompletableFuture<Reply> future = new CompletableFuture<>();
                        future.complete(new ErrorReply("ERR keys in request not in same resources"));
                        return future;
                    }
                }
                Resource readResource = ResourceChooser.getReadResource(resources);
                return doRead(readResource, commandFlusher, command);
            } else {
                List<Resource> resources = resourceChooser.getWriteResources(objects[3]);
                for (int i = 4; i < 3 + keyCount; i++) {
                    byte[] key = objects[i];
                    List<Resource> writeResources = resourceChooser.getWriteResources(key);
                    boolean checkWriteResourcesEqual = ResourceChooser.checkWriteResourcesEqual(resources, writeResources);
                    if (!checkWriteResourcesEqual) {
                        CompletableFuture<Reply> future = new CompletableFuture<>();
                        future.complete(new ErrorReply("ERR keys in request not in same resources"));
                        return future;
                    }
                }
                return doWrite(resources, commandFlusher, command);
            }
        }
    }

    private CompletableFuture<Reply> mset(Command command, CommandFlusher commandFlusher) {
        byte[][] args = command.getObjects();
        if ((args.length - 1) % 2 != 0) {
            CompletableFuture<Reply> future = new CompletableFuture<>();
            future.complete(new ErrorReply("wrong number of arguments for 'mset' command"));
            return future;
        }
        Set<String> firstWriteResourceUrls = new HashSet<>();
        Map<String, List<byte[]>> map = new HashMap<>();
        for (int i = 1; i < args.length; i++, i++) {
            byte[] key = args[i];
            byte[] value = args[i + 1];
            List<Resource> resources = getWriteResources(key);
            for (int j = 0; j < resources.size(); j++) {
                Resource resource = resources.get(j);
                List<byte[]> list = map.get(resource.getUrl());
                if (list == null) {
                    list = new ArrayList<>();
                    list.add(args[0]);
                    map.put(resource.getUrl(), list);
                }
                list.add(key);
                list.add(value);
                if (j == 0) {
                    firstWriteResourceUrls.add(resource.getUrl());
                }
            }
        }
        List<CompletableFuture<Reply>> futures = new ArrayList<>();
        List<CompletableFuture<Reply>> allFutures = new ArrayList<>();
        for (Map.Entry<String, List<byte[]>> entry : map.entrySet()) {
            String url = entry.getKey();
            List<byte[]> list = entry.getValue();
            AsyncClient client = factory.get(url);
            Command subCommand = new Command(list.toArray(new byte[0][0]));
            subCommand.setChannelInfo(command.getChannelInfo());
            CompletableFuture<Reply> future = commandFlusher.sendCommand(client, subCommand);
            incrWrite(url, command);
            if (firstWriteResourceUrls.contains(url)) {
                futures.add(future);
            }
            allFutures.add(future);
        }
        if (multiWriteMode == MultiWriteMode.FIRST_RESOURCE_ONLY) {
            if (futures.size() == 1) {
                return futures.get(0);
            }
            CompletableFuture<Reply> future = new CompletableFuture<>();
            AsyncUtils.allOf(futures).thenAccept(replies -> future.complete(Utils.mergeStatusReply(replies)));
            return future;
        }
        CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
        AsyncUtils.allOf(allFutures).thenAccept(replies -> {
            if (multiWriteMode == MultiWriteMode.ALL_RESOURCES_CHECK_ERROR) {
                for (Reply reply : replies) {
                    if (reply instanceof ErrorReply) {
                        completableFuture.complete(reply);
                        return;
                    }
                }
            }
            if (futures.size() == 1) {
                futures.get(0).thenAccept(completableFuture::complete);
                return;
            }
            CompletableFuture<Reply> future = new CompletableFuture<>();
            AsyncUtils.allOf(futures).thenAccept(replies1 -> future.complete(Utils.mergeStatusReply(replies1)));
            future.thenAccept(completableFuture::complete);
        });
        return completableFuture;
    }

    private CompletableFuture<Reply> mget(Command command, CommandFlusher commandFlusher) {
        List<BytesKey> redisKeys = new ArrayList<>();
        Map<String, List<BytesKey>> map = new HashMap<>();
        byte[][] args = command.getObjects();
        for (int i = 1; i < args.length; i++) {
            byte[] key = args[i];
            BytesKey redisKey = new BytesKey(key);
            redisKeys.add(redisKey);
            Resource resource = getReadResource(key);
            List<BytesKey> list = map.get(resource.getUrl());
            if (list == null) {
                list = map.computeIfAbsent(resource.getUrl(), k -> new ArrayList<>(args.length - 1));
            }
            list.add(redisKey);
        }
        List<String> urlList = new ArrayList<>();
        List<CompletableFuture<Reply>> futures = new ArrayList<>();
        for (Map.Entry<String, List<BytesKey>> entry : map.entrySet()) {
            String url = entry.getKey();
            AsyncClient client = factory.get(url);
            List<BytesKey> list = entry.getValue();
            List<byte[]> subCommandArgs = new ArrayList<>(list.size() + 1);
            subCommandArgs.add(args[0]);
            for (BytesKey redisKey : list) {
                subCommandArgs.add(redisKey.getKey());
            }
            urlList.add(url);
            Command subCommand = new Command(subCommandArgs.toArray(new byte[0][0]));
            subCommand.setChannelInfo(command.getChannelInfo());
            CompletableFuture<Reply> future = commandFlusher.sendCommand(client, subCommand);
            incrRead(url, command);
            futures.add(future);
        }
        if (futures.size() == 1) {
            return futures.get(0);
        }
        CompletableFuture<Reply> future = new CompletableFuture<>();
        AsyncUtils.allOf(futures).thenAccept(replies -> {
            Map<BytesKey, Reply> replyMap = new HashMap<>();
            for (int i = 0; i < urlList.size(); i++) {
                String url = urlList.get(i);
                List<BytesKey> keyList = map.get(url);
                Reply reply = replies.get(i);
                if (reply instanceof MultiBulkReply) {
                    MultiBulkReply multiBulkReply = (MultiBulkReply) reply;
                    Reply[] subReplies = multiBulkReply.getReplies();
                    for (int j = 0; j < subReplies.length; j++) {
                        Reply subReply = subReplies[j];
                        BytesKey redisKey = keyList.get(j);
                        replyMap.put(redisKey, subReply);
                    }
                } else if (reply instanceof ErrorReply) {
                    future.complete(reply);
                    return;
                } else {
                    future.complete(ErrorReply.NOT_AVAILABLE);
                    return;
                }
            }
            Reply[] retReplies = new Reply[redisKeys.size()];
            for (int i = 0; i < redisKeys.size(); i++) {
                BytesKey redisKey = redisKeys.get(i);
                retReplies[i] = replyMap.get(redisKey);
            }
            future.complete(new MultiBulkReply(retReplies));
        });
        return future;
    }

    private CompletableFuture<Reply> delOrUnlinkOrTouch(Command command, CommandFlusher commandFlusher) {
        byte[][] args = command.getObjects();
        List<CompletableFuture<Reply>> futures = new ArrayList<>();
        List<CompletableFuture<Reply>> allFutures = new ArrayList<>();
        //拆成N个命令进行投递，不做聚合，因为如果分片逻辑下，可能某个resource同时为双写的第1个地址和第2个地址
        for (int i = 1; i < args.length; i++) {
            byte[] key = args[i];
            List<Resource> resources = getWriteResources(key);
            for (int j = 0; j < resources.size(); j++) {
                Resource resource = resources.get(j);
                AsyncClient client = factory.get(resource.getUrl());
                byte[][] subCommandArg = new byte[][]{args[0], key};
                Command subCommand = new Command(subCommandArg);
                subCommand.setChannelInfo(command.getChannelInfo());
                CompletableFuture<Reply> future = commandFlusher.sendCommand(client, subCommand);
                incrWrite(resource, command);
                if (j == 0) {
                    futures.add(future);
                }
                allFutures.add(future);
            }
        }
        if (multiWriteMode == MultiWriteMode.FIRST_RESOURCE_ONLY) {
            if (futures.size() == 1) {
                return futures.get(0);
            }
            CompletableFuture<Reply> future = new CompletableFuture<>();
            AsyncUtils.allOf(futures).thenAccept(replies -> future.complete(Utils.mergeIntegerReply(replies)));
            return future;
        } else {
            CompletableFuture<List<Reply>> all = AsyncUtils.allOf(allFutures);
            CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
            all.thenAccept(replies -> {
                if (multiWriteMode == MultiWriteMode.ALL_RESOURCES_CHECK_ERROR) {
                    for (Reply reply : replies) {
                        if (reply instanceof ErrorReply) {
                            completableFuture.complete(reply);
                            return;
                        }
                    }
                }
                if (futures.size() == 1) {
                    CompletableFuture<Reply> future1 = futures.get(0);
                    future1.thenAccept(completableFuture::complete);
                    return;
                }
                CompletableFuture<Reply> future = new CompletableFuture<>();
                AsyncUtils.allOf(futures).thenAccept(replies1 -> future.complete(Utils.mergeIntegerReply(replies1)));
                future.thenAccept(completableFuture::complete);
            });
            return completableFuture;
        }
    }

    private CompletableFuture<Reply> exists(Command command, CommandFlusher commandFlusher) {
        Map<String, List<byte[]>> map = new HashMap<>();
        byte[][] args = command.getObjects();
        for (int i = 1; i < args.length; i++) {
            byte[] key = args[i];
            Resource resource = getReadResource(key);
            List<byte[]> list = map.get(resource.getUrl());
            if (list == null) {
                list = new ArrayList<>();
                list.add(args[0]);
                map.put(resource.getUrl(), list);
            }
            list.add(key);
        }
        List<CompletableFuture<Reply>> futures = new ArrayList<>();
        for (Map.Entry<String, List<byte[]>> entry : map.entrySet()) {
            String url = entry.getKey();
            List<byte[]> list = entry.getValue();
            AsyncClient client = factory.get(url);

            Command subCommand = new Command(list.toArray(new byte[0][0]));
            subCommand.setChannelInfo(command.getChannelInfo());
            CompletableFuture<Reply> future = commandFlusher.sendCommand(client, subCommand);
            incrRead(url, command);
            futures.add(future);
        }
        if (futures.size() == 1) {
            return futures.get(0);
        }
        CompletableFuture<Reply> future = new CompletableFuture<>();
        AsyncUtils.allOf(futures).thenAccept(replies -> future.complete(Utils.mergeIntegerReply(replies)));
        return future;
    }

    private CompletableFuture<Reply> readCommandWithDynamicKeyCount(Command command, CommandFlusher commandFlusher, int start, int end) {
        byte[][] objects = command.getObjects();
        if (resourceChooser.getType() == ResourceTable.Type.SHADING) {
            String url = null;
            for (int i = start; i <= end; i++) {
                byte[] key = objects[i];
                Resource resource = resourceChooser.getReadResource(key);
                if (url != null && !url.equals(resource.getUrl())) {
                    CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
                    completableFuture.complete(new ErrorReply("ERR keys in request not in same resources"));
                    return completableFuture;
                }
                url = resource.getUrl();
            }
            AsyncClient client = factory.get(url);
            incrRead(url, command);
            return commandFlusher.sendCommand(client, command);
        } else {
            Resource resource = resourceChooser.getReadResource(Utils.EMPTY_ARRAY);
            return doRead(resource, commandFlusher, command);
        }
    }

    private CompletableFuture<Reply> msetnx(Command command, CommandFlusher commandFlusher) {
        CompletableFuture<Reply> future;
        byte[][] objects = command.getObjects();
        if (objects.length < 3 || objects.length % 2 != 1) {
            future = new CompletableFuture<>();
            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
        } else {
            ResourceTable.Type type = resourceChooser.getType();
            if (type == ResourceTable.Type.SIMPLE) {
                List<Resource> writeResources = resourceChooser.getWriteResources(Utils.EMPTY_ARRAY);
                future = doWrite(writeResources, commandFlusher, command);
            } else if (type == ResourceTable.Type.SHADING) {
                List<Resource> writeResources = null;
                for (int i = 1; i < objects.length; i += 2) {
                    byte[] key = objects[i];
                    List<Resource> resources = resourceChooser.getWriteResources(key);
                    if (writeResources != null) {
                        boolean writeResourcesEqual = ResourceChooser.checkWriteResourcesEqual(writeResources, resources);
                        if (!writeResourcesEqual) {
                            future = new CompletableFuture<>();
                            future.complete(new ErrorReply("ERR keys in request not in same resources"));
                            return future;
                        }
                    }
                    writeResources = resources;
                }
                future = doWrite(writeResources, commandFlusher, command);
            } else {
                future = new CompletableFuture<>();
                future.complete(ErrorReply.NOT_SUPPORT);
            }
        }
        return future;
    }

    private CompletableFuture<Reply> bitop(Command command, CommandFlusher commandFlusher) {
        CompletableFuture<Reply> future;
        byte[][] objects = command.getObjects();
        if (objects.length < 4) {
            future = new CompletableFuture<>();
            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
        } else {
            future = writeCommandWithDynamicKeyCount(command, commandFlusher, 2, objects.length - 1);
        }
        return future;
    }

    private CompletableFuture<Reply> zinterstoreOrZunionstore(Command command, CommandFlusher commandFlusher) {
        CompletableFuture<Reply> future;
        byte[][] objects = command.getObjects();
        if (objects.length < 4) {
            future = new CompletableFuture<>();
            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
        } else {
            int keyCount = (int) Utils.bytesToNum(objects[2]);
            if (keyCount <= 0) {
                future = new CompletableFuture<>();
                future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
            } else {
                future = writeCommandWithDynamicKeyCount(command, commandFlusher, 3, 3 + keyCount, objects[1]);
            }
        }
        return future;
    }

    private CompletableFuture<Reply> writeCommandWithDynamicKeyCount(Command command, CommandFlusher commandFlusher, int start, int end, byte[]... otherKeys) {
        byte[][] objects = command.getObjects();
        ResourceTable.Type type = resourceChooser.getType();
        if (type == ResourceTable.Type.SIMPLE) {
            List<Resource> writeResources = resourceChooser.getWriteResources(Utils.EMPTY_ARRAY);
            return doWrite(writeResources, commandFlusher, command);
        } else if (type == ResourceTable.Type.SHADING) {
            if (objects.length <= end) {
                CompletableFuture<Reply> future = new CompletableFuture<>();
                future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
                return future;
            }
            List<Resource> writeResources = null;
            for (int i = start; i <= end; i++) {
                byte[] key = objects[i];
                List<Resource> resources = resourceChooser.getWriteResources(key);
                if (writeResources != null) {
                    boolean writeResourcesEqual = ResourceChooser.checkWriteResourcesEqual(writeResources, resources);
                    if (!writeResourcesEqual) {
                        CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
                        completableFuture.complete(new ErrorReply("ERR keys in request not in same resources"));
                        return completableFuture;
                    }
                }
                writeResources = resources;
            }
            if (otherKeys != null && writeResources != null) {
                for (byte[] otherKey : otherKeys) {
                    List<Resource> resources = resourceChooser.getWriteResources(otherKey);
                    boolean writeResourcesEqual = ResourceChooser.checkWriteResourcesEqual(writeResources, resources);
                    if (!writeResourcesEqual) {
                        CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
                        completableFuture.complete(new ErrorReply("ERR keys in request not in same resources"));
                        return completableFuture;
                    }
                    writeResources = resources;
                }
            }
            if (writeResources == null || writeResources.isEmpty()) {
                CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
                completableFuture.complete(new ErrorReply("ERR keys in request not in same resources"));
                return completableFuture;
            }
            if (writeResources.size() > 1 && command.isBlocking()) {
                CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
                completableFuture.complete(new ErrorReply("ERR blocking command do not support multi-write"));
                return completableFuture;
            }
            return doWrite(writeResources, commandFlusher, command);
        } else {
            CompletableFuture<Reply> future = new CompletableFuture<>();
            future.complete(ErrorReply.NOT_SUPPORT);
            return future;
        }
    }

    private synchronized void init(ResourceTable resourceTable) {
        this.resourceChooser = new ResourceChooser(resourceTable, env.getProxyEnv());

        ResourceTable.Type type = resourceChooser.getType();
        if (type == ResourceTable.Type.SHADING) {
            isSingletonStandaloneRedisOrRedisSentinel = false;
            isSingletonStandaloneRedisOrRedisSentinelOrRedisCluster = false;
            return;
        }
        Set<Resource> allResources = resourceChooser.getAllResources();
        if (allResources.size() != 1) {
            isSingletonStandaloneRedisOrRedisSentinel = false;
            isSingletonStandaloneRedisOrRedisSentinelOrRedisCluster = false;
            return;
        }
        try {
            singletonClient = factory.get(resourceChooser.getReadResource(Utils.EMPTY_ARRAY).getUrl());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            singletonClient = null;
        }
        isSingletonStandaloneRedisOrRedisSentinelOrRedisCluster = true;
        for (Resource resource : allResources) {
            String url = resource.getUrl();
            if (url.startsWith(RedisType.Redis.getPrefix()) || url.startsWith(RedisType.RedisSentinel.getPrefix())) {
                isSingletonStandaloneRedisOrRedisSentinel = true;
                return;
            }
        }
        isSingletonStandaloneRedisOrRedisSentinel = false;
    }

    private static class ProxyRouteConfUpdaterReloadTask implements Runnable {

        private final AtomicBoolean running = new AtomicBoolean(false);
        private final AsyncCamelliaRedisTemplate template;
        private final long bid;
        private final String bgroup;
        private final ProxyRouteConfUpdater updater;
        private ResourceTable resourceTable;

        ProxyRouteConfUpdaterReloadTask(AsyncCamelliaRedisTemplate template, ResourceTable resourceTable, long bid, String bgroup,
                                        ProxyRouteConfUpdater updater) {
            this.template = template;
            this.resourceTable = resourceTable;
            this.bid = bid;
            this.bgroup = bgroup;
            this.updater = updater;
        }

        @Override
        public void run() {
            if (running.compareAndSet(false, true)) {
                try {
                    ResourceTable resourceTable = updater.getResourceTable(bid, bgroup);
                    try {
                        RedisResourceUtil.checkResourceTable(resourceTable);
                    } catch (Exception e) {
                        logger.error("resourceTable check error, skip reload, bid = {}, bgroup = {}, resourceTable = {}",
                                bid, bgroup, ReadableResourceTableUtil.readableResourceTable(PasswordMaskUtils.maskResourceTable(resourceTable)), e);
                        return;
                    }
                    String newJson = ReadableResourceTableUtil.readableResourceTable(resourceTable);
                    String oldJson = ReadableResourceTableUtil.readableResourceTable(this.resourceTable);
                    if (!newJson.equals(oldJson)) {
                        template.init(resourceTable);
                        this.resourceTable = resourceTable;
                        if (logger.isInfoEnabled()) {
                            logger.info("reload success, bid = {}, bgroup = {}, resourceTable = {}", bid, bgroup,
                                    ReadableResourceTableUtil.readableResourceTable(PasswordMaskUtils.maskResourceTable(resourceTable)));
                        }
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("not modify, skip reload, bid = {}, bgroup = {}", bid, bgroup);
                        }
                    }
                } catch (Exception e) {
                    Throwable ex = ErrorHandlerUtil.handler(e);
                    String log = "reload error, bid = " + bid + ", bgroup = " + bgroup + ", updater = " + updater.getClass().getName() + ", ex = " + ex.toString();
                    ErrorLogCollector.collect(AsyncCamelliaRedisTemplate.class, log, e);
                } finally {
                    running.set(false);
                }
            } else {
                logger.warn("ProxyRouteConfUpdaterReloadTask is running, skip run, bid = {}, bgroup = {}, updater = {}", bid, bgroup, updater.getClass().getName());
            }
        }
    }

    private static class ReloadTask implements Runnable {

        private final AtomicBoolean running = new AtomicBoolean(false);

        private final AsyncCamelliaRedisTemplate template;
        private final CamelliaApi service;
        private final long bid;
        private final String bgroup;
        private String md5;

        ReloadTask(AsyncCamelliaRedisTemplate template, CamelliaApi service, long bid, String bgroup, String md5) {
            this.template = template;
            this.service = service;
            this.bid = bid;
            this.bgroup = bgroup;
            this.md5 = md5;
        }

        @Override
        public void run() {
            if (running.compareAndSet(false, true)) {
                try {
                    CamelliaApiResponse response = service.getResourceTable(bid, bgroup, md5);
                    if (response.getCode() == CamelliaApiCode.NOT_MODIFY.getCode()) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("not modify, bid = {}, bgroup = {}, md5 = {}", bid, bgroup, md5);
                        }
                        return;
                    }
                    try {
                        RedisResourceUtil.checkResourceTable(response.getResourceTable());
                    } catch (Exception e) {
                        logger.error("resourceTable check error, skip reload, bid = {}, bgroup = {}, resourceTable = {}",
                                bid, bgroup, ReadableResourceTableUtil.readableResourceTable(PasswordMaskUtils.maskResourceTable(response.getResourceTable())), e);
                        return;
                    }
                    template.init(response.getResourceTable());
                    this.md5 = response.getMd5();
                    if (logger.isInfoEnabled()) {
                        logger.info("reload success, bid = {}, bgroup = {}, md5 = {}, resourceTable = {}", bid, bgroup, md5,
                                ReadableResourceTableUtil.readableResourceTable(PasswordMaskUtils.maskResourceTable(response.getResourceTable())));
                    }
                } catch (Exception e) {
                    Throwable ex = ErrorHandlerUtil.handler(e);
                    String log = "reload error, bid = " + bid + ", bgroup = " + bgroup + ", md5 = " + md5 + ", ex = " + ex.toString();
                    ErrorLogCollector.collect(AsyncCamelliaRedisTemplate.class, log, e);
                } finally {
                    running.set(false);
                }
            } else {
                logger.warn("ReloadTask is running, skip run, bid = {}, bgroup = {}, md5 = {}", bid, bgroup, md5);
            }
        }
    }

    private Resource getReadResource(byte[] key) {
        return resourceChooser.getReadResource(key);
    }

    private List<Resource> getWriteResources(byte[] key) {
        return resourceChooser.getWriteResources(key);
    }

    private Resource getReadResource(Command command) {
        byte[] key = command.getObjects()[1];
        return resourceChooser.getReadResource(key);
    }

    private List<Resource> getWriteResources(Command command) {
        byte[] key = command.getObjects()[1];
        return resourceChooser.getWriteResources(key);
    }

    private static final String className = AsyncCamelliaRedisTemplate.class.getSimpleName();

    private void incrRead(String url, Command command) {
        if (monitor != null) {
            monitor.incrRead(url, className, command.getName());
        }
        if (bid == -1) {
            ResourceStatsMonitor.incr(null, null, url, command.getName());
        } else {
            ResourceStatsMonitor.incr(bid, bgroup, url, command.getName());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("read command = {}, bid = {}, bgroup = {}, resource = {}", command.getName(), bid, bgroup, url);
        }
    }

    private void incrRead(Resource resource, Command command) {
        if (monitor != null) {
            monitor.incrRead(resource.getUrl(), className, command.getName());
        }
        if (bid == -1) {
            ResourceStatsMonitor.incr(null, null, resource.getUrl(), command.getName());
        } else {
            ResourceStatsMonitor.incr(bid, bgroup, resource.getUrl(), command.getName());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("read command = {}, bid = {}, bgroup = {}, resource = {}", command.getName(), bid, bgroup, resource.getUrl());
        }
    }

    private void incrWrite(String url, Command command) {
        if (monitor != null) {
            monitor.incrWrite(url, className, command.getName());
        }
        if (bid == -1) {
            ResourceStatsMonitor.incr(null, null, url, command.getName());
        } else {
            ResourceStatsMonitor.incr(bid, bgroup, url, command.getName());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("write command = {}, bid = {}, bgroup = {}, resource = {}", command.getName(), bid, bgroup, url);
        }
    }

    private void incrWrite(Resource resource, Command command) {
        if (monitor != null) {
            monitor.incrWrite(resource.getUrl(), className, command.getName());
        }
        if (bid == -1) {
            ResourceStatsMonitor.incr(null, null, resource.getUrl(), command.getName());
        } else {
            ResourceStatsMonitor.incr(bid, bgroup, resource.getUrl(), command.getName());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("write command = {}, bid = {}, bgroup = {}, resource = {}", command.getName(), bid, bgroup, resource.getUrl());
        }
    }
}
