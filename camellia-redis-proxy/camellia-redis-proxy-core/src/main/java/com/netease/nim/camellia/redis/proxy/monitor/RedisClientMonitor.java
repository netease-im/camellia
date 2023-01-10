package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.RedisConnectStats;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClient;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientAddr;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientConfig;
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
public class RedisClientMonitor {

    private static final Logger logger = LoggerFactory.getLogger(RedisClientMonitor.class);

    private static final ConcurrentHashMap<RedisClientAddr, ConcurrentHashMap<String, RedisClient>> redisClientMap = new ConcurrentHashMap<>();

    /**
     * 统计RedisClient，增加
     * @param redisClient RedisClient
     */
    public static void addRedisClient(RedisClient redisClient) {
        try {
            ExecutorUtils.submitToSingleThreadExecutor(() -> {
                try {
                    RedisClientConfig config = redisClient.getRedisClientConfig();
                    RedisClientAddr addr = new RedisClientAddr(config.getHost(), config.getPort(), config.getUserName(), config.getPassword());
                    ConcurrentHashMap<String, RedisClient> subMap = redisClientMap.get(addr);
                    if (subMap == null) {
                        subMap = new ConcurrentHashMap<>();
                        redisClientMap.put(addr, subMap);
                    }
                    subMap.put(redisClient.getClientName(), redisClient);
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
     * @param redisClient RedisClient
     */
    public static void removeRedisClient(RedisClient redisClient) {
        try {
            ExecutorUtils.submitToSingleThreadExecutor(() -> {
                try {
                    RedisClientConfig config = redisClient.getRedisClientConfig();
                    RedisClientAddr addr = new RedisClientAddr(config.getHost(), config.getPort(), config.getUserName(), config.getPassword());
                    ConcurrentHashMap<String, RedisClient> subMap = redisClientMap.get(addr);
                    if (subMap != null) {
                        subMap.remove(redisClient.getClientName());
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

    public static ConcurrentHashMap<RedisClientAddr, ConcurrentHashMap<String, RedisClient>> getRedisClientMap() {
        return redisClientMap;
    }

    public static RedisConnectStats collect() {
        RedisConnectStats redisConnectStats = new RedisConnectStats();
        List<RedisConnectStats.Detail> detailList = new ArrayList<>();
        for (Map.Entry<RedisClientAddr, ConcurrentHashMap<String, RedisClient>> entry : redisClientMap.entrySet()) {
            RedisClientAddr key = entry.getKey();
            ConcurrentHashMap<String, RedisClient> subMap = entry.getValue();
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
