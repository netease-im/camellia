package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.RedisConnectStats;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionConfig;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统计到后端redis的连接数
 */
public class RedisConnectionMonitor {

    private static final Logger logger = LoggerFactory.getLogger(RedisConnectionMonitor.class);

    private static final ConcurrentHashMap<RedisConnectionAddr, ConcurrentHashMap<String, RedisConnection>> redisClientMap = new ConcurrentHashMap<>();

    /**
     * 统计RedisClient，增加
     * @param redisConnection RedisClient
     */
    public static void addRedisConnection(RedisConnection redisConnection) {
        try {
            ExecutorUtils.submitToSingleThreadExecutor(() -> {
                try {
                    RedisConnectionConfig config = redisConnection.getConfig();
                    RedisConnectionAddr addr = new RedisConnectionAddr(config.getHost(), config.getPort(), config.getUserName(), config.getPassword());
                    ConcurrentHashMap<String, RedisConnection> subMap = redisClientMap.get(addr);
                    if (subMap == null) {
                        subMap = new ConcurrentHashMap<>();
                        redisClientMap.put(addr, subMap);
                    }
                    subMap.put(redisConnection.getConnectionName(), redisConnection);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 统计RedisClient，减少
     * @param redisConnection RedisClient
     */
    public static void removeRedisConnection(RedisConnection redisConnection) {
        try {
            ExecutorUtils.submitToSingleThreadExecutor(() -> {
                try {
                    RedisConnectionConfig config = redisConnection.getConfig();
                    RedisConnectionAddr addr = new RedisConnectionAddr(config.getHost(), config.getPort(), config.getUserName(), config.getPassword());
                    ConcurrentHashMap<String, RedisConnection> subMap = redisClientMap.get(addr);
                    if (subMap != null) {
                        subMap.remove(redisConnection.getConnectionName());
                        if (subMap.isEmpty()) {
                            redisClientMap.remove(addr);
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static ConcurrentHashMap<RedisConnectionAddr, ConcurrentHashMap<String, RedisConnection>> getRedisClientMap() {
        return redisClientMap;
    }

    public static RedisConnectStats collect() {
        RedisConnectStats redisConnectStats = new RedisConnectStats();
        List<RedisConnectStats.Detail> detailList = new ArrayList<>();
        for (Map.Entry<RedisConnectionAddr, ConcurrentHashMap<String, RedisConnection>> entry : redisClientMap.entrySet()) {
            RedisConnectionAddr key = entry.getKey();
            ConcurrentHashMap<String, RedisConnection> subMap = entry.getValue();
            if (subMap.isEmpty()) continue;
            redisConnectStats.setConnectCount(redisConnectStats.getConnectCount() + subMap.size());
            RedisConnectStats.Detail detail = new RedisConnectStats.Detail();
            detail.setAddr(PasswordMaskUtils.maskAddr(key.getUrl()));
            detail.setConnectCount(subMap.size());
            detailList.add(detail);
        }
        redisConnectStats.setDetailList(detailList);
        return redisConnectStats;
    }
}
