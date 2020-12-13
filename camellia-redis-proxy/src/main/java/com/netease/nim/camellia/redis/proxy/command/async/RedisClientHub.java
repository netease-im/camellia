package com.netease.nim.camellia.redis.proxy.command.async;


import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.FastThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2019/12/18.
 */
public class RedisClientHub {

    private static final Logger logger = LoggerFactory.getLogger(RedisClientHub.class);

    private static final ConcurrentHashMap<String, RedisClient> map = new ConcurrentHashMap<>();
    public static NioEventLoopGroup eventLoopGroup = null;
    public static NioEventLoopGroup eventLoopGroupBackup = null;

    private static final FastThreadLocal<ConcurrentHashMap<String, RedisClient>> threadLocalMap = new FastThreadLocal<>();
    private static final FastThreadLocal<EventLoop> eventLoopThreadLocal = new FastThreadLocal<>();

    private static final ConcurrentHashMap<String, AtomicLong> failCountMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> failTimestampMap = new ConcurrentHashMap<>();

    public static int heartbeatIntervalSeconds = Constants.Transpond.heartbeatIntervalSeconds;
    public static long heartbeatTimeoutMillis = Constants.Transpond.heartbeatTimeoutMillis;
    public static int connectTimeoutMillis = Constants.Transpond.connectTimeoutMillis;
    public static int failCountThreshold = Constants.Transpond.failCountThreshold;
    public static long failBanMillis = Constants.Transpond.failBanMillis;

    private static final Object lock = new Object();

    public static void updateEventLoop(EventLoop eventLoop) {
        eventLoopThreadLocal.set(eventLoop);
    }

    public static RedisClient get(String host, int port, String password) {
        RedisClientAddr addr = new RedisClientAddr(host, port, password);
        return get(addr);
    }

    public static RedisClient newClient(String host, int port, String password) {
        return newClient(new RedisClientAddr(host, port, password));
    }

    public static RedisClient newClient(RedisClientAddr addr) {
        String url = addr.getUrl();
        if (fastFail(url)) {
            return null;
        }
        EventLoop loopGroup = eventLoopGroup.next();
        if (loopGroup.inEventLoop()) {
            loopGroup = eventLoopGroupBackup.next();
        }
        RedisClient client = new RedisClient(addr.getHost(), addr.getPort(), addr.getPassword(),
                loopGroup, -1, -1, connectTimeoutMillis);
        client.start();
        if (client.isValid()) {
            resetFail(url);//如果client初始化成功，则重置计数器和错误时间戳
            return client;
        } else {
            client.stop();
            incrFail(url);
            return null;
        }
    }

    public static RedisClient get(RedisClientAddr addr) {
        RedisClient cache = addr.getCache();
        if (cache != null && cache.isValid()) {
            return cache;
        }

        EventLoopGroup loopGroup;

        ConcurrentHashMap<String, RedisClient> map;
        EventLoop eventLoop = eventLoopThreadLocal.get();
        if (eventLoop == null) {
            map = RedisClientHub.map;
            EventLoop next = eventLoopGroup.next();
            if (next.inEventLoop()) {
                next = eventLoopGroup.next();
                if (next.inEventLoop()) {
                    loopGroup = eventLoopGroupBackup.next();
                } else {
                    loopGroup = next;
                }
            } else {
                loopGroup = next;
            }
        } else if (eventLoop.inEventLoop()) {
            map = RedisClientHub.map;
            loopGroup = eventLoopGroup.next();
        } else {
            map = threadLocalMap.get();
            if (map == null) {
                map = new ConcurrentHashMap<>();
                threadLocalMap.set(map);
            }
            loopGroup = eventLoop;
        }

        String url = addr.getUrl();
        RedisClient client = map.get(url);
        if (client == null) {
            if (fastFail(url)) {
                return null;
            }
            synchronized (lock) {
                client = map.get(url);
                if (client == null) {
                    client = new RedisClient(addr.getHost(), addr.getPort(), addr.getPassword(), loopGroup,
                            heartbeatIntervalSeconds, heartbeatTimeoutMillis, connectTimeoutMillis);
                    client.start();
                    if (client.isValid()) {
                        RedisClient oldClient = map.put(url, client);
                        if (oldClient != null) {
                            oldClient.stop();
                        }
                        resetFail(url);//如果client初始化成功，则重置计数器和错误时间戳
                    } else {
                        incrFail(url);//client初始化失败，递增错误计数器
                        client.stop();
                    }
                }
            }
        }
        if (client.isValid()) {
            addr.setCache(client);
            return client;
        } else {
            if (fastFail(url)) {
                return null;
            }
            synchronized (lock) {
                client = map.get(url);
                if (client != null && client.isValid()) {
                    return client;
                }
                if (client != null && !client.isValid()) {
                    client.stop();
                }
                client = new RedisClient(addr.getHost(), addr.getPort(), addr.getPassword(), loopGroup,
                        heartbeatIntervalSeconds, heartbeatTimeoutMillis, connectTimeoutMillis);
                client.start();
                if (client.isValid()) {
                    RedisClient oldClient = map.put(url, client);
                    if (oldClient != null) {
                        oldClient.stop();
                    }
                    resetFail(url);//如果client初始化成功，则重置计数器和错误时间戳
                    addr.setCache(client);
                    return client;
                } else {
                    incrFail(url);//client初始化失败，递增错误计数器
                    client.stop();
                }
            }
        }
        String log = "get RedisClient fail, key = " + url;
        ErrorLogCollector.collect(RedisClientHub.class, log);
        return null;
    }

    private static boolean fastFail(String url) {
        //如果client处于不可用状态，检查不可用时长
        long failTimestamp = getFailTimestamp(url);
        if (TimeCache.currentMillis - failTimestamp < failBanMillis) {
            //如果错误时间戳在禁用时间范围内，则直接返回null
            //此时去重置一下计数器，这样确保failBanMillis到期之后failCount从0开始计算
            resetFailCount(url);
            String log = "currentTimeMillis - failTimestamp < failBanMillis[" + failBanMillis + "], immediate return null, key = " + url;
            ErrorLogCollector.collect(RedisClientHub.class, log);
            return true;
        }
        long failCount = getFailCount(url);
        if (failCount > failCountThreshold) {
            //如果错误次数超过了阈值，则设置当前时间为错误时间戳，并重置计数器
            //接下来的failBanMillis时间内，都会直接返回null
            setFailTimestamp(url);
            resetFailCount(url);
            String log = "failCount > failCountThreshold[" + failCountThreshold + "], immediate return null, key = " + url;
            ErrorLogCollector.collect(RedisClientHub.class, log);
            return true;
        }
        return false;
    }

    private static final HashedWheelTimer timer = new HashedWheelTimer();
    public static void delayStopIfIdle(RedisClient redisClient) {
        try {
            IdleCheckTask task = new IdleCheckTask(redisClient);
            submitIdleCheckTask(task);
        } catch (Exception e) {
            logger.error("delayStopIfIdle error, client = {}", redisClient.getClientName(), e);
        }
    }

    private static void submitIdleCheckTask(IdleCheckTask task) {
        timer.newTimeout(timeout -> {
            try {
                Boolean success = task.call();
                if (!success) {
                    submitIdleCheckTask(task);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }, 1, TimeUnit.MINUTES);
    }

    private static class IdleCheckTask implements Callable<Boolean> {
        private final RedisClient redisClient;
        public IdleCheckTask(RedisClient redisClient) {
            this.redisClient = redisClient;
        }
        @Override
        public Boolean call() {
            try {
                if (!redisClient.isValid()) {
                    redisClient.stop();
                    return true;
                }
                if (redisClient.isIdle()) {
                    redisClient.stop(true);
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return true;
            }
        }
    }

    private static long getFailTimestamp(String key) {
        AtomicLong failTimestamp = failTimestampMap.computeIfAbsent(key, k -> new AtomicLong(0L));
        return failTimestamp.get();
    }

    private static void setFailTimestamp(String key) {
        AtomicLong failTimestamp = failTimestampMap.computeIfAbsent(key, k -> new AtomicLong(0L));
        failTimestamp.set(TimeCache.currentMillis);
    }

    private static void resetFailTimestamp(String key) {
        AtomicLong failCount = failTimestampMap.computeIfAbsent(key, k -> new AtomicLong(0L));
        failCount.set(0L);
    }

    private static void resetFailCount(String key) {
        AtomicLong failCount = failCountMap.computeIfAbsent(key, k -> new AtomicLong());
        failCount.set(0L);
    }

    private static long getFailCount(String key) {
        AtomicLong failCount = failCountMap.computeIfAbsent(key, k -> new AtomicLong());
        return failCount.get();
    }

    private static void incrFail(String key) {
        AtomicLong failCount = failCountMap.computeIfAbsent(key, k -> new AtomicLong());
        failCount.incrementAndGet();
    }

    private static void resetFail(String key) {
        resetFailTimestamp(key);
        resetFailCount(key);
    }
}
