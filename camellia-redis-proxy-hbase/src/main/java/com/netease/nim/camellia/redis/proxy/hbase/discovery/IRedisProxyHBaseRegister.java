package com.netease.nim.camellia.redis.proxy.hbase.discovery;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * Created by caojiajun on 2020/5/14.
 */
public interface IRedisProxyHBaseRegister {

    void register();

    void deregister();

    int instanceCount();

    public static class Default implements IRedisProxyHBaseRegister {

        private static final Logger logger = LoggerFactory.getLogger(Default.class);
        private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private final AtomicBoolean renewThreadStart = new AtomicBoolean(false);
        private ScheduledFuture<?> scheduledFuture;
        private final Object lock = new Object();

        private final CamelliaRedisTemplate redisTemplate;
        private final String registerKey;
        private final String instanceUrl;

        public Default(CamelliaRedisTemplate redisTemplate, String registerKey, String instanceUrl) {
            this.redisTemplate = redisTemplate;
            this.registerKey = registerKey;
            this.instanceUrl = instanceUrl;
        }

        @Override
        public void register() {
            synchronized (lock) {
                _updateRedis();
                if (renewThreadStart.compareAndSet(false, true)) {
                    int registerRenewIntervalSeconds = RedisHBaseConfiguration.registerRenewIntervalSeconds();
                    scheduledFuture = scheduler.scheduleAtFixedRate(this::renew, registerRenewIntervalSeconds, registerRenewIntervalSeconds, TimeUnit.SECONDS);
                    logger.info("register to redis success, registerKey = {}, instanceUrl = {}, registerRenewIntervalSeconds = {}",
                            registerKey, instanceUrl, registerRenewIntervalSeconds);
                }
            }
        }

        @Override
        public void deregister() {
            synchronized (lock) {
                if (renewThreadStart.compareAndSet(true, false)) {
                    scheduledFuture.cancel(false);
                }
                redisTemplate.zrem(registerKey, instanceUrl);
                logger.warn("deregister to redis success, registerKey = {}, instanceUrl = {}", registerKey, instanceUrl);
            }
        }

        @Override
        public int instanceCount() {
            redisTemplate.zremrangeByScore(registerKey,
                    0, System.currentTimeMillis() - RedisHBaseConfiguration.registerExpireSeconds() * 1000);
            return redisTemplate.zcard(registerKey).intValue();
        }

        private void renew() {
            synchronized (lock) {
                try {
                    _updateRedis();
                    if (logger.isDebugEnabled()) {
                        logger.debug("renew success, registerKey = {}, instanceUrl = {}", registerKey, instanceUrl);
                    }
                } catch (Exception e) {
                    logger.error("renew error, registerKey = {}, instanceUrl = {}", registerKey, instanceUrl, e);
                }
            }
        }

        private void _updateRedis() {
            try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
                pipeline.zadd(registerKey, System.currentTimeMillis(), instanceUrl);
                pipeline.zremrangeByScore(registerKey,
                        0, System.currentTimeMillis() - RedisHBaseConfiguration.registerExpireSeconds() * 1000);
                pipeline.expire(registerKey, RedisHBaseConfiguration.registerExpireSeconds() * 10);
                pipeline.sync();
            }
        }
    }
}
