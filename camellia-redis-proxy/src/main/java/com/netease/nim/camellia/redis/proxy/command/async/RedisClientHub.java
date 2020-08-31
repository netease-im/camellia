package com.netease.nim.camellia.redis.proxy.command.async;


import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.netty.ServerStatus;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.FastThreadLocal;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 * Created by caojiajun on 2019/12/18.
 */
public class RedisClientHub {

    private static final ConcurrentHashMap<String, RedisClient> map = new ConcurrentHashMap<>();
    public static NioEventLoopGroup eventLoopGroup = null;

    private static final FastThreadLocal<ConcurrentHashMap<String, RedisClient>> threadLocalMap = new FastThreadLocal<>();
    private static final FastThreadLocal<EventLoop> eventLoopThreadLocal = new FastThreadLocal<>();

    private static final ConcurrentHashMap<String, LongAdder> failCountMap = new ConcurrentHashMap<>();
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
        RedisClient client = new RedisClient(addr.getHost(), addr.getPort(), addr.getPassword(),
                eventLoopGroup, -1, -1, connectTimeoutMillis);
        client.start();
        if (client.isValid()) {
            return client;
        } else {
            client.stop();
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

        if (eventLoopThreadLocal.get().inEventLoop()) {
            loopGroup = eventLoopGroup;
            map = RedisClientHub.map;
        } else {
            loopGroup = eventLoopThreadLocal.get();
            map = threadLocalMap.get();
            if (map == null) {
                map = new ConcurrentHashMap<>();
                threadLocalMap.set(map);
            }
        }

        String url = addr.getUrl();
        RedisClient client = map.get(url);
        if (client == null) {
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
            //如果client处于不可用状态，检查不可用时长
            long failTimestamp = getFailTimestamp(url);
            if (ServerStatus.getCurrentTimeMillis() - failTimestamp < failBanMillis) {
                //如果错误时间戳在禁用时间范围内，则直接返回null
                //此时去重置一下计数器，这样确保failBanMillis到期之后failCount从0开始计算
                resetFailCount(url);
                String log = "currentTimeMillis - failTimestamp < failBanMillis[" + failBanMillis + "], immediate return null, key = " + url;
                ErrorLogCollector.collect(RedisClientHub.class, log);
                return null;
            }
            long failCount = getFailCount(url);
            if (failCount > failCountThreshold) {
                //如果错误次数超过了阈值，则设置当前时间为错误时间戳，并重置计数器
                //接下来的failBanMillis时间内，都会直接返回null
                setFailTimestamp(url);
                resetFailCount(url);
                String log = "failCount > failCountThreshold[" + failCountThreshold + "], immediate return null, key = " + url;
                ErrorLogCollector.collect(RedisClientHub.class, log);
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
