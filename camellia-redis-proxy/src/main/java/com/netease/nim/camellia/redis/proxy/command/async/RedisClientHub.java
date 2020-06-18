package com.netease.nim.camellia.redis.proxy.command.async;


import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.netty.ServerStatus;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.core.util.SysUtils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 * Created by caojiajun on 2019/12/18.
 */
public class RedisClientHub {

    private static final ConcurrentHashMap<String, RedisClient> map = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> failCountMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> failTimestampMap = new ConcurrentHashMap<>();

    private static final ExecutorService exec = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuNum(), 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1024), new CamelliaThreadFactory(RedisClientHub.class));

    private static final Object lock = new Object();

    public static int heartbeatIntervalSeconds = Constants.Async.heartbeatIntervalSeconds;
    public static long heartbeatTimeoutMillis = Constants.Async.heartbeatTimeoutMillis;
    public static int commandPipelineFlushThreshold = Constants.Async.commandPipelineFlushThreshold;
    public static int connectTimeoutMillis = Constants.Async.connectTimeoutMillis;
    public static int failCountThreshold = Constants.Async.failCountThreshold;
    public static long failBanMillis = Constants.Async.failBanMillis;

    public static CompletableFuture<RedisClient> getAsync(String host, int port, String password) {
        String key = (password == null ? "" : password) + "@" + host + ":" + port;
        RedisClient client = map.get(key);
        CompletableFuture<RedisClient> future = new CompletableFuture<>();
        if (client != null && client.isValid()) {
            future.complete(client);
        } else {
            try {
                exec.submit(() -> {
                    RedisClient redisClient = get(host, port, password);
                    future.complete(redisClient);
                });
            } catch (Exception e) {
                String log = "submit exec error, key = " + key;
                ErrorLogCollector.collect(RedisClientHub.class, log);
                future.complete(null);
            }
        }
        return future;
    }

    public static RedisClient get(String host, int port, String password) {
        String key = (password == null ? "" : password) + "@" + host + ":" + port;
        RedisClient client = map.get(key);
        if (client == null) {
            synchronized (lock) {
                client = map.get(key);
                if (client == null) {
                    client = new RedisClient(host, port, password,
                            heartbeatIntervalSeconds, heartbeatTimeoutMillis, commandPipelineFlushThreshold, connectTimeoutMillis);
                    client.start();
                    if (client.isValid()) {
                        RedisClient oldClient = map.put(key, client);
                        if (oldClient != null) {
                            oldClient.stop();
                        }
                        resetFail(key);//如果client初始化成功，则重置计数器和错误时间戳
                    } else {
                        incrFail(key);//client初始化失败，递增错误计数器
                        client.stop();
                    }
                }
            }
        }
        if (client.isValid()) {
            return client;
        } else {
            //如果client处于不可用状态，检查不可用时长
            long failTimestamp = getFailTimestamp(key);
            if (ServerStatus.getCurrentTimeMillis() - failTimestamp < failBanMillis) {
                //如果错误时间戳在禁用时间范围内，则直接返回null
                //此时去重置一下计数器，这样确保failBanMillis到期之后failCount从0开始计算
                resetFailCount(key);
                String log = "currentTimeMillis - failTimestamp < failBanMillis[" + failBanMillis + "], immediate return null, key = " + key;
                ErrorLogCollector.collect(RedisClientHub.class, log);
                return null;
            }
            long failCount = getFailCount(key);
            if (failCount > failCountThreshold) {
                //如果错误次数超过了阈值，则设置当前时间为错误时间戳，并重置计数器
                //接下来的failBanMillis时间内，都会直接返回null
                setFailTimestamp(key);
                resetFailCount(key);
                String log = "failCount > failCountThreshold[" + failCountThreshold + "], immediate return null, key = " + key;
                ErrorLogCollector.collect(RedisClientHub.class, log);
                return null;
            }
            synchronized (lock) {
                client = map.get(key);
                if (client != null && client.isValid()) {
                    return client;
                }
                if (client != null && !client.isValid()) {
                    client.stop();
                }
                client = new RedisClient(host, port, password,
                        heartbeatIntervalSeconds, heartbeatTimeoutMillis, commandPipelineFlushThreshold, connectTimeoutMillis);
                client.start();
                if (client.isValid()) {
                    RedisClient oldClient = map.put(key, client);
                    if (oldClient != null) {
                        oldClient.stop();
                    }
                    resetFail(key);//如果client初始化成功，则重置计数器和错误时间戳
                    return client;
                } else {
                    incrFail(key);//client初始化失败，递增错误计数器
                    client.stop();
                }
            }
        }
        String log = "get RedisClient fail, key = " + key;
        ErrorLogCollector.collect(RedisClientHub.class, log);
        return null;
    }

    private static long getFailTimestamp(String key) {
        AtomicLong failTimestamp = failTimestampMap.computeIfAbsent(key, k -> new AtomicLong(0L));
        return failTimestamp.get();
    }

    private static void setFailTimestamp(String key) {
        AtomicLong failTimestamp = failTimestampMap.computeIfAbsent(key, k -> new AtomicLong(0L));
        failTimestamp.set(ServerStatus.getCurrentTimeMillis());
    }

    private static void resetFailTimestamp(String key) {
        AtomicLong failCount = failTimestampMap.computeIfAbsent(key, k -> new AtomicLong(0L));
        failCount.set(0L);
    }

    private static void resetFailCount(String key) {
        LongAdder failCount = failCountMap.computeIfAbsent(key, k -> new LongAdder());
        failCount.reset();
    }

    private static long getFailCount(String key) {
        LongAdder failCount = failCountMap.computeIfAbsent(key, k -> new LongAdder());
        return failCount.sum();
    }

    private static void incrFail(String key) {
        LongAdder failCount = failCountMap.computeIfAbsent(key, k -> new LongAdder());
        failCount.increment();
    }

    private static void resetFail(String key) {
        resetFailTimestamp(key);
        resetFailCount(key);
    }
}
