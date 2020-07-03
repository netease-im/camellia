package com.netease.nim.camellia.redis.proxy.hbase.util;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;


/**
 *
 * Created by caojiajun on 2020/6/28.
 */
public class FreqUtil {

    private static final Logger logger = LoggerFactory.getLogger(FreqUtil.class);

    public static boolean freq(CamelliaRedisTemplate redisTemplate, byte[] key, int threshold, long expireMillis) {
        if (!RedisHBaseConfiguration.freqEnable()) {
            return true;
        }
        try {
            Long incr = redisTemplate.incr(key);
            if (incr == 1) {
                redisTemplate.pexpire(key, expireMillis);
            }
            if (incr > threshold && incr % (threshold * 10) == 0) {
                Long ttl = redisTemplate.ttl(key);
                if (ttl == -1) {
                    redisTemplate.pexpire(key, expireMillis);
                }
            }
            return incr <= threshold;
        } catch (Exception e) {
            logger.error("freq error, key = {}, threshold = {}, expireMillis = {}, default.pass = {}, ex = {}",
                    SafeEncoder.encode(key), threshold, expireMillis, RedisHBaseConfiguration.freqDefaultPass(), e.toString());
            return RedisHBaseConfiguration.freqDefaultPass();
        }
    }
}
