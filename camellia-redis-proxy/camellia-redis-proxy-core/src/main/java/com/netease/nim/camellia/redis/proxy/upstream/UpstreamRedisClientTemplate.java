package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.core.api.*;
import com.netease.nim.camellia.core.client.env.Monitor;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.*;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.ProxyRouteType;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.route.ProxyRouteConfUpdater;
import com.netease.nim.camellia.redis.proxy.conf.MultiWriteMode;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.monitor.*;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.utils.CompletableFutureUtils;
import com.netease.nim.camellia.redis.proxy.upstream.utils.ScanCursorCalculator;
import com.netease.nim.camellia.redis.proxy.util.*;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.BytesKey;
import com.netease.nim.camellia.tools.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2019/12/19.
 */
public class UpstreamRedisClientTemplate implements IUpstreamRedisClientTemplate {

    private static final Logger logger = LoggerFactory.getLogger(UpstreamRedisClientTemplate.class);

    private static final ScheduledExecutorService scheduleExecutor
            = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(UpstreamRedisClientTemplate.class));

    private static final long defaultBid = -1;
    private static final String defaultBgroup = "local";
    private static final long defaultCheckIntervalMillis = 5000;
    private static final boolean defaultMonitorEnable = false;

    private final UpstreamRedisClientFactory factory;
    private ScheduledFuture<?> future;

    private final long bid;
    private final String bgroup;
    private final MultiWriteMode multiWriteMode;

    private RedisProxyEnv env;
    private Monitor monitor;
    private ResourceSelector resourceSelector;

    private ProxyRouteType proxyRouteType;
    private IUpstreamClient singletonClient;

    private ScanCursorCalculator cursorCalculator;

    private ResourceSelector.ResourceChecker resourceChecker;

    public UpstreamRedisClientTemplate(ResourceTable resourceTable) {
        this(RedisProxyEnv.defaultRedisEnv(), resourceTable);
    }

    public UpstreamRedisClientTemplate(RedisProxyEnv env, ResourceTable resourceTable) {
        this(env, new LocalCamelliaApi(resourceTable), defaultBid, defaultBgroup, defaultMonitorEnable, defaultCheckIntervalMillis, false);
    }

    public UpstreamRedisClientTemplate(RedisProxyEnv env, String resourceTableFilePath, long checkIntervalMillis) {
        this(env, new ReloadableLocalFileCamelliaApi(resourceTableFilePath, RedisResourceUtil.RedisResourceTableChecker),
                defaultBid, defaultBgroup, defaultMonitorEnable, checkIntervalMillis, true);
    }

    public UpstreamRedisClientTemplate(RedisProxyEnv env, CamelliaApi service, long bid, String bgroup,
                                       boolean monitorEnable, long checkIntervalMillis) {
        this(env, service, bid, bgroup, monitorEnable, checkIntervalMillis, true);
    }

    public UpstreamRedisClientTemplate(RedisProxyEnv env, CamelliaApi service, long bid, String bgroup,
                                       boolean monitorEnable, long checkIntervalMillis, boolean reload) {
        this.env = env;
        this.bid = bid;
        this.bgroup = bgroup;
        this.factory = env.getClientFactory();
        this.resourceChecker = env.getResourceChecker();
        this.multiWriteMode = env.getMultiWriteMode();
        CamelliaApiResponse response = service.getResourceTable(bid, bgroup, null);
        String md5 = response.getMd5();
        if (response.getResourceTable() == null) {
            throw new CamelliaRedisException("resourceTable is null");
        }
        RedisResourceUtil.checkResourceTable(response.getResourceTable());
        this.update(response.getResourceTable());
        if (logger.isInfoEnabled()) {
            logger.info("UpstreamRedisClientTemplate init success, bid = {}, bgroup = {}, md5 = {}, resourceTable = {}", bid, bgroup, md5,
                    ReadableResourceTableUtil.readableResourceTable(PasswordMaskUtils.maskResourceTable(response.getResourceTable())));
        }
        if (reload) {
            DashboardReloadTask dashboardReloadTask = new DashboardReloadTask(this, service, bid, bgroup, md5);
            this.future = scheduleExecutor.scheduleAtFixedRate(dashboardReloadTask, checkIntervalMillis, checkIntervalMillis, TimeUnit.MILLISECONDS);
            if (monitorEnable) {
                Monitor monitor = new RemoteMonitor(bid, bgroup, service);
                ProxyEnv proxyEnv = new ProxyEnv.Builder(env.getProxyEnv()).monitor(monitor).build();
                this.env = new RedisProxyEnv.Builder(env).proxyEnv(proxyEnv).build();
                this.monitor = monitor;
            }
        }
        if (bid == -1) {
            RouteConfMonitor.registerRedisTemplate(null, null, this);
        } else {
            RouteConfMonitor.registerRedisTemplate(bid, bgroup, this);
        }
    }

    public UpstreamRedisClientTemplate(RedisProxyEnv env, long bid, String bgroup, ProxyRouteConfUpdater updater, long reloadIntervalMillis) {
        this.env = env;
        this.bid = bid;
        this.bgroup = bgroup;
        this.factory = env.getClientFactory();
        this.multiWriteMode = env.getMultiWriteMode();
        ResourceTable resourceTable = updater.getResourceTable(bid, bgroup);
        RedisResourceUtil.checkResourceTable(resourceTable);
        this.update(resourceTable);
        if (logger.isInfoEnabled()) {
            logger.info("AsyncCamelliaRedisTemplate init success, bid = {}, bgroup = {}, resourceTable = {}, ProxyRouteConfUpdater = {}", bid, bgroup,
                    ReadableResourceTableUtil.readableResourceTable(PasswordMaskUtils.maskResourceTable(resourceTable)), updater.getClass().getName());
        }
        if (reloadIntervalMillis > 0) {
            ProxyRouteConfUpdaterReloadTask reloadTask = new ProxyRouteConfUpdaterReloadTask(this, resourceTable, bid, bgroup, updater);
            this.future = scheduleExecutor.scheduleAtFixedRate(reloadTask, reloadIntervalMillis, reloadIntervalMillis, TimeUnit.MILLISECONDS);
        }
        if (bid == -1) {
            RouteConfMonitor.registerRedisTemplate(null, null, this);
        } else {
            RouteConfMonitor.registerRedisTemplate(bid, bgroup, this);
        }
    }

    @Override
    public List<CompletableFuture<Reply>> sendCommand(List<Command> commands) {
        List<CompletableFuture<Reply>> futureList = new ArrayList<>(commands.size());

        if (isPassThroughCommand(commands)) {
            String url = resourceSelector.getReadResource(Utils.EMPTY_ARRAY).getUrl();
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
                IUpstreamClient client = factory.get(url);
                client.sendCommand(commands, futureList);
            }
            return futureList;
        }

        CommandFlusher commandFlusher = new CommandFlusher(commands.size());
        for (Command command : commands) {
            RedisCommand redisCommand = command.getRedisCommand();

            if (redisCommand.getSupportType() == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_1) {
                if (resourceSelector.getResourceTable().getType() == ResourceTable.Type.SHADING) {
                    ChannelInfo channelInfo = command.getChannelInfo();
                    channelInfo.setInSubscribe(false);
                    if (!channelInfo.isInTransaction()) {
                        RedisConnection bindClient = channelInfo.getBindClient();
                        if (bindClient != null) {
                            channelInfo.setBindClient(-1, null);
                            bindClient.startIdleCheck();
                        }
                    }
                    CompletableFuture<Reply> future = new CompletableFuture<>();
                    future.complete(ErrorReply.NOT_SUPPORT);
                    futureList.add(future);
                    continue;
                }
                if (redisCommand == RedisCommand.SUBSCRIBE || redisCommand == RedisCommand.PSUBSCRIBE
                        || redisCommand == RedisCommand.UNSUBSCRIBE || redisCommand == RedisCommand.PUNSUBSCRIBE) {
                    List<Resource> writeResources = resourceSelector.getWriteResources(Utils.EMPTY_ARRAY);
                    if (writeResources == null || writeResources.isEmpty()) {
                        ChannelInfo channelInfo = command.getChannelInfo();
                        channelInfo.setInSubscribe(false);
                        if (!channelInfo.isInTransaction()) {
                            RedisConnection bindClient = channelInfo.getBindClient();
                            if (bindClient != null) {
                                channelInfo.setBindClient(-1, null);
                                bindClient.startIdleCheck();
                            }
                        }
                        CompletableFuture<Reply> future = new CompletableFuture<>();
                        future.complete(ErrorReply.NOT_AVAILABLE);
                        futureList.add(future);
                        continue;
                    }
                    CompletableFuture<Reply> future;
                    if (writeResources.size() != 1) {
                        Resource resource = writeResources.get(0);
                        future = doWrite(Collections.singletonList(resource), commandFlusher, command);
                    } else {
                        future = doWrite(writeResources, commandFlusher, command);
                    }
                    futureList.add(future);
                    continue;
                }
                //other commands
            }

            if (redisCommand.getSupportType() == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_2) {
                if (proxyRouteType != ProxyRouteType.REDIS_STANDALONE && proxyRouteType != ProxyRouteType.REDIS_SENTINEL
                        && proxyRouteType != ProxyRouteType.REDIS_CLUSTER) {
                    CompletableFuture<Reply> future = new CompletableFuture<>();
                    future.complete(ErrorReply.NOT_SUPPORT);
                    futureList.add(future);
                    continue;
                }
                Resource resource = resourceSelector.getReadResource(Utils.EMPTY_ARRAY);
                String url = resource.getUrl();
                IUpstreamClient client = factory.get(url);
                CompletableFuture<Reply> future = commandFlusher.sendCommand(client, command);
                if (redisCommand.getType() == RedisCommand.Type.READ) {
                    incrRead(url, command);
                } else if (redisCommand.getType() == RedisCommand.Type.WRITE) {
                    incrWrite(url, command);
                }
                futureList.add(future);
                continue;
            }

            if (redisCommand.getSupportType() == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_3) {
                if (proxyRouteType != ProxyRouteType.REDIS_STANDALONE && proxyRouteType != ProxyRouteType.REDIS_SENTINEL) {
                    CompletableFuture<Reply> future = new CompletableFuture<>();
                    future.complete(ErrorReply.NOT_SUPPORT);
                    futureList.add(future);
                    continue;
                }
                Resource resource = resourceSelector.getReadResource(Utils.EMPTY_ARRAY);
                String url = resource.getUrl();
                IUpstreamClient client = factory.get(url);
                CompletableFuture<Reply> future = commandFlusher.sendCommand(client, command);
                if (redisCommand.getType() == RedisCommand.Type.READ) {
                    incrRead(url, command);
                } else if (redisCommand.getType() == RedisCommand.Type.WRITE) {
                    incrWrite(url, command);
                }
                futureList.add(future);
                continue;
            }

            if (redisCommand.getSupportType() == RedisCommand.CommandSupportType.RESTRICTIVE_SUPPORT) {
                CompletableFuture<Reply> future = null;
                if (redisCommand.getType() == RedisCommand.Type.READ) {
                    Resource resource;
                    List<byte[]> keys = command.getKeys();
                    if (keys.isEmpty()) {
                        resource = resourceSelector.getReadResource(Utils.EMPTY_ARRAY);
                    } else if (keys.size() == 1) {
                        resource = resourceSelector.getReadResource(keys.get(0));
                    } else {
                        resource = resourceSelector.getReadResourceWithCheckEqual(keys);
                    }
                    if (resource == null) {
                        future = new CompletableFuture<>();
                        future.complete(new ErrorReply("ERR keys in request not in same resources"));
                    } else {
                        future = doRead(resource, commandFlusher, command);
                    }
                } else if (redisCommand.getType() == RedisCommand.Type.WRITE) {
                    List<Resource> writeResources;
                    List<byte[]> keys = command.getKeys();
                    if (keys.isEmpty()) {
                        writeResources = resourceSelector.getWriteResources(Utils.EMPTY_ARRAY);
                    } else if (keys.size() == 1) {
                        writeResources = resourceSelector.getWriteResources(keys.get(0));
                    } else {
                        writeResources = resourceSelector.getWriteResourcesWithCheckEqual(keys);
                    }
                    if (writeResources == null) {
                        future = new CompletableFuture<>();
                        future.complete(new ErrorReply("ERR keys in request not in same resources"));
                    } else if (writeResources.size() > 1) {
                        if (command.isBlocking()) {
                            future = new CompletableFuture<>();
                            future.complete(new ErrorReply("ERR blocking command do not support multi-write"));
                        }
                    }
                    if (future == null) {
                        future = doWrite(writeResources, commandFlusher, command);
                    }
                }
                if (future == null) {
                    future = new CompletableFuture<>();
                    future.complete(ErrorReply.SYNTAX_ERROR);
                }
                futureList.add(future);
                continue;
            }

            if (redisCommand == RedisCommand.SCAN) {
                try {
                    if (proxyRouteType == ProxyRouteType.REDIS_STANDALONE || proxyRouteType == ProxyRouteType.REDIS_SENTINEL
                            || proxyRouteType == ProxyRouteType.REDIS_CLUSTER) {
                        Resource resource = resourceSelector.getReadResource(Utils.EMPTY_ARRAY);
                        CompletableFuture<Reply> future = doRead(resource, commandFlusher, command);
                        futureList.add(future);
                    } else {
                        List<Resource> allReadResources = resourceSelector.getAllReadResources();
                        if (cursorCalculator == null || cursorCalculator.getNodeBitLen() != ScanCursorCalculator.getSuitableNodeBitLen(allReadResources.size())) {
                            cursorCalculator = new ScanCursorCalculator(ScanCursorCalculator.getSuitableNodeBitLen(allReadResources.size()));
                        }
                        byte[][] objects = command.getObjects();
                        if (objects == null || objects.length <= 1) {
                            CompletableFuture<Reply> future = new CompletableFuture<>();
                            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
                            futureList.add(future);
                            continue;
                        }
                        int currentNodeIndex = cursorCalculator.filterScanCommand(command);
                        if (currentNodeIndex < 0) {
                            CompletableFuture<Reply> future = new CompletableFuture<>();
                            future.complete(ErrorReply.argNumWrong(command.getRedisCommand()));
                            futureList.add(future);
                            continue;
                        }
                        if (currentNodeIndex >= allReadResources.size()) {
                            CompletableFuture<Reply> future = new CompletableFuture<>();
                            future.complete(new ErrorReply("ERR illegal arguments of cursor"));
                            futureList.add(future);
                            continue;
                        }
                        Resource resource = allReadResources.get(currentNodeIndex);
                        IUpstreamClient client = factory.get(resource.getUrl());
                        CompletableFuture<Reply> f = commandFlusher.sendCommand(client, command);
                        CompletableFuture<Reply> future = new CompletableFuture<>();
                        f.thenApply((reply) -> cursorCalculator.filterScanReply(reply, currentNodeIndex, allReadResources.size())).thenAccept(future::complete);
                        futureList.add(future);
                        incrRead(resource.getUrl(), command);
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
                CompletableFuture<Reply> future;
                String ope = Utils.bytesToString(objects[1]);
                if (ope.equalsIgnoreCase(RedisKeyword.FLUSH.name())
                        || ope.equalsIgnoreCase(RedisKeyword.LOAD.name())) {
                    List<Resource> resources = resourceSelector.getAllWriteResources();
                    future = doWrite(resources, commandFlusher, command);
                } else if (ope.equalsIgnoreCase(RedisKeyword.EXISTS.name())) {
                    List<Resource> resources = resourceSelector.getAllReadResources();
                    List<CompletableFuture<Reply>> futures = new ArrayList<>();
                    for (Resource resource : resources) {
                        CompletableFuture<Reply> subFuture = doRead(resource, commandFlusher, command);
                        futures.add(subFuture);
                    }
                    future = new CompletableFuture<>();
                    CompletableFutureUtils.allOf(futures).thenAccept(replies -> future.complete(Utils.mergeMultiIntegerReply(replies)));
                } else {
                    future = new CompletableFuture<>();
                    future.complete(ErrorReply.NOT_SUPPORT);
                }
                futureList.add(future);
                continue;
            }

            if (resourceSelector.getType() == ResourceTable.Type.SHADING) {
                //这些命令比较特殊，需要把后端的结果merge起来之后再返回给客户端
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
                    case JSON_MGET: {
                        if (command.getObjects().length > 3) {
                            CompletableFuture<Reply> future = jsonMget(command, commandFlusher);
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

            RedisCommand.Type type = redisCommand.getType();
            byte[][] args = command.getObjects();
            if (type == RedisCommand.Type.READ) {
                Resource resource;
                if (redisCommand.getCommandKeyType() == RedisCommand.CommandKeyType.SIMPLE_SINGLE && args.length >= 2) {
                    resource = resourceSelector.getReadResource(args[1]);
                } else {
                    List<byte[]> keys = command.getKeys();
                    if (keys.isEmpty()) {
                        resource = resourceSelector.getReadResource(Utils.EMPTY_ARRAY);
                    } else {//按道理走到这里的都是只有一个key的命令，且不是blocking的
                        resource = resourceSelector.getReadResource(keys.get(0));
                    }
                }
                CompletableFuture<Reply> future = doRead(resource, commandFlusher, command);
                futureList.add(future);
            } else if (type == RedisCommand.Type.WRITE) {
                List<Resource> writeResources;
                if (redisCommand.getCommandKeyType() == RedisCommand.CommandKeyType.SIMPLE_SINGLE && args.length >= 2) {
                    writeResources = resourceSelector.getWriteResources(args[1]);
                } else {
                    List<byte[]> keys = command.getKeys();
                    if (keys.isEmpty()) {
                        writeResources = resourceSelector.getWriteResources(Utils.EMPTY_ARRAY);
                    } else {//按道理走到这里的都是只有一个key的命令，且不是blocking的
                        writeResources = resourceSelector.getWriteResources(keys.get(0));
                    }
                }
                CompletableFuture<Reply> future = doWrite(writeResources, commandFlusher, command);
                futureList.add(future);
            } else {
                throw new CamelliaRedisException("not support RedisCommand.Type");
            }
        }
        commandFlusher.flush();
        return futureList;
    }

    @Override
    public ResourceTable getResourceTable() {
        return resourceSelector.getResourceTable();
    }

    @Override
    public long getResourceTableUpdateTime() {
        return resourceSelector.getCreateTime();
    }

    @Override
    public void preheat() {
        Set<Resource> allResources = this.resourceSelector.getAllResources();
        for (Resource resource : allResources) {
            IUpstreamClient client = factory.get(resource.getUrl());
            client.preheat();
        }
    }

    @Override
    public synchronized void update(ResourceTable resourceTable) {
        List<Resource> oldWriteResources = null;
        if (this.resourceSelector != null) {
            oldWriteResources = this.resourceSelector.getWriteResources(Utils.EMPTY_ARRAY);
        }
        RedisResourceUtil.checkResourceTable(resourceTable);
        //初始化每个Resource，会做基本的校验
        Set<Resource> resources = ResourceUtil.getAllResources(resourceTable);
        for (Resource resource : resources) {
            factory.get(resource.getUrl());
        }
        //初始化IUpstreamClientTemplate
        this.resourceSelector = new ResourceSelector(resourceTable, env.getProxyEnv(), resourceChecker);

        this.proxyRouteType = ProxyRouteType.fromResourceTable(resourceTable);
        if (this.proxyRouteType == ProxyRouteType.REDIS_STANDALONE ||
                this.proxyRouteType == ProxyRouteType.REDIS_SENTINEL ||
                this.proxyRouteType == ProxyRouteType.REDIS_CLUSTER) {
            try {
                singletonClient = factory.get(resourceSelector.getReadResource(Utils.EMPTY_ARRAY).getUrl());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                singletonClient = null;
            }
        }
        //check need force close subscribe channel
        boolean needCloseSubscribeChannel = this.resourceSelector.getResourceTable().getType() == ResourceTable.Type.SHADING;
        if (!needCloseSubscribeChannel) {
            List<Resource> newWriteResources = this.resourceSelector.getWriteResources(Utils.EMPTY_ARRAY);
            needCloseSubscribeChannel = oldWriteResources != null && !oldWriteResources.isEmpty() && !newWriteResources.isEmpty()
                    && !ResourceUtil.resourceEquals(oldWriteResources.get(0), newWriteResources.get(0));
        }
        if (needCloseSubscribeChannel) {
            Set<ChannelInfo> channelMap = ChannelMonitor.getChannelMap(bid, bgroup);
            for (ChannelInfo channelInfo : channelMap) {
                try {
                    if (channelInfo.isInSubscribe()) {
                        logger.warn("route conf update, force close subscribe channel = {}, consid = {}", channelInfo.getCtx().channel(), channelInfo.getConsid());
                        channelInfo.getCtx().close();
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void shutdown() {
        if (future != null) {
            future.cancel(false);
        }
        if (bid == -1) {
            RouteConfMonitor.deregisterRedisTemplate(null, null);
        } else {
            RouteConfMonitor.deregisterRedisTemplate(bid, bgroup);
        }
        if (logger.isInfoEnabled()) {
            logger.info("AsyncCamelliaRedisTemplate shutdown, bid = {}, bgroup = {}", bid, bgroup);
        }
    }

    private boolean isPassThroughCommand(List<Command> commands) {
        if (proxyRouteType == ProxyRouteType.COMPLEX) return false;
        for (Command command : commands) {
            RedisCommand redisCommand = command.getRedisCommand();
            if (redisCommand.getSupportType() == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_1
                    || redisCommand.getSupportType() == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_2
                    || redisCommand.getSupportType() == RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_3) {
                return false;
            }
        }
        return true;
    }

    private CompletableFuture<Reply> doRead(Resource resource, CommandFlusher commandFlusher, Command command) {
        IUpstreamClient client = factory.get(resource.getUrl());
        CompletableFuture<Reply> future = commandFlusher.sendCommand(client, command);
        incrRead(resource.getUrl(), command);
        return future;
    }

    private CompletableFuture<Reply> doWrite(List<Resource> writeResources, CommandFlusher commandFlusher, Command command) {
        List<CompletableFuture<Reply>> list = new ArrayList<>(writeResources.size());
        for (Resource resource : writeResources) {
            IUpstreamClient client = factory.get(resource.getUrl());
            CompletableFuture<Reply> future = commandFlusher.sendCommand(client, command);
            incrWrite(resource.getUrl(), command);
            list.add(future);
        }
        return CompletableFutureUtils.finalReply(list, multiWriteMode);
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
            List<Resource> resources = resourceSelector.getWriteResources(key);
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
            IUpstreamClient client = factory.get(url);
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
            CompletableFutureUtils.allOf(futures).thenAccept(replies -> future.complete(Utils.mergeStatusReply(replies)));
            return future;
        }
        CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
        CompletableFutureUtils.allOf(allFutures).thenAccept(replies -> {
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
            CompletableFutureUtils.allOf(futures).thenAccept(replies1 -> future.complete(Utils.mergeStatusReply(replies1)));
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
            Resource resource = resourceSelector.getReadResource(key);
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
            IUpstreamClient client = factory.get(url);
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
        CompletableFutureUtils.allOf(futures).thenAccept(replies -> {
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

    private CompletableFuture<Reply> jsonMget(Command command, CommandFlusher commandFlusher) {
        List<BytesKey> redisKeys = new ArrayList<>();
        Map<String, List<BytesKey>> map = new HashMap<>();
        byte[][] args = command.getObjects();
        byte[] path = args[args.length - 1];
        for (int i = 1; i < args.length - 1; i++) {
            byte[] key = args[i];
            BytesKey redisKey = new BytesKey(key);
            redisKeys.add(redisKey);
            Resource resource = resourceSelector.getReadResource(key);
            List<BytesKey> list = map.get(resource.getUrl());
            if (list == null) {
                list = map.computeIfAbsent(resource.getUrl(), k -> new ArrayList<>(args.length - 2));
            }
            list.add(redisKey);
        }
        List<String> urlList = new ArrayList<>();
        List<CompletableFuture<Reply>> futures = new ArrayList<>();
        for (Map.Entry<String, List<BytesKey>> entry : map.entrySet()) {
            String url = entry.getKey();
            IUpstreamClient client = factory.get(url);
            List<BytesKey> list = entry.getValue();
            List<byte[]> subCommandArgs = new ArrayList<>(list.size() + 1);
            subCommandArgs.add(args[0]);
            for (BytesKey redisKey : list) {
                subCommandArgs.add(redisKey.getKey());
            }
            subCommandArgs.add(path);
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
        CompletableFutureUtils.allOf(futures).thenAccept(replies -> {
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
            List<Resource> resources = resourceSelector.getWriteResources(key);
            for (int j = 0; j < resources.size(); j++) {
                Resource resource = resources.get(j);
                IUpstreamClient client = factory.get(resource.getUrl());
                byte[][] subCommandArg = new byte[][]{args[0], key};
                Command subCommand = new Command(subCommandArg);
                subCommand.setChannelInfo(command.getChannelInfo());
                CompletableFuture<Reply> future = commandFlusher.sendCommand(client, subCommand);
                incrWrite(resource.getUrl(), command);
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
            CompletableFutureUtils.allOf(futures).thenAccept(replies -> future.complete(Utils.mergeIntegerReply(replies)));
            return future;
        } else {
            CompletableFuture<List<Reply>> all = CompletableFutureUtils.allOf(allFutures);
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
                CompletableFutureUtils.allOf(futures).thenAccept(replies1 -> future.complete(Utils.mergeIntegerReply(replies1)));
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
            Resource resource = resourceSelector.getReadResource(key);
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
            IUpstreamClient client = factory.get(url);

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
        CompletableFutureUtils.allOf(futures).thenAccept(replies -> future.complete(Utils.mergeIntegerReply(replies)));
        return future;
    }



    private static class ProxyRouteConfUpdaterReloadTask implements Runnable {

        private final AtomicBoolean running = new AtomicBoolean(false);
        private final UpstreamRedisClientTemplate template;
        private final long bid;
        private final String bgroup;
        private final ProxyRouteConfUpdater updater;
        private ResourceTable resourceTable;

        ProxyRouteConfUpdaterReloadTask(UpstreamRedisClientTemplate template, ResourceTable resourceTable, long bid, String bgroup,
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
                        template.update(resourceTable);
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
                    Throwable ex = ExceptionUtils.onError(e);
                    String log = "reload error, bid = " + bid + ", bgroup = " + bgroup + ", updater = " + updater.getClass().getName() + ", ex = " + ex.toString();
                    ErrorLogCollector.collect(UpstreamRedisClientTemplate.class, log, e);
                } finally {
                    running.set(false);
                }
            } else {
                logger.warn("ProxyRouteConfUpdaterReloadTask is running, skip run, bid = {}, bgroup = {}, updater = {}", bid, bgroup, updater.getClass().getName());
            }
        }
    }

    private static class DashboardReloadTask implements Runnable {

        private final AtomicBoolean running = new AtomicBoolean(false);

        private final UpstreamRedisClientTemplate template;
        private final CamelliaApi service;
        private final long bid;
        private final String bgroup;
        private String md5;

        DashboardReloadTask(UpstreamRedisClientTemplate template, CamelliaApi service, long bid, String bgroup, String md5) {
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
                    template.update(response.getResourceTable());
                    this.md5 = response.getMd5();
                    if (logger.isInfoEnabled()) {
                        logger.info("reload success, bid = {}, bgroup = {}, md5 = {}, resourceTable = {}", bid, bgroup, md5,
                                ReadableResourceTableUtil.readableResourceTable(PasswordMaskUtils.maskResourceTable(response.getResourceTable())));
                    }
                } catch (Exception e) {
                    Throwable ex = ExceptionUtils.onError(e);
                    String log = "reload error, bid = " + bid + ", bgroup = " + bgroup + ", md5 = " + md5 + ", ex = " + ex.toString();
                    ErrorLogCollector.collect(UpstreamRedisClientTemplate.class, log, e);
                } finally {
                    running.set(false);
                }
            } else {
                logger.warn("DashboardReloadTask is running, skip run, bid = {}, bgroup = {}, md5 = {}", bid, bgroup, md5);
            }
        }
    }

    private static final String className = UpstreamRedisClientTemplate.class.getSimpleName();

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

}
