package com.netease.nim.camellia.id.gen.strict;

import com.netease.nim.camellia.core.util.CacheUtil;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * 严格递增的发号器（仅依赖redis）
 * <p>
 * 使用当前时间戳（秒）作为前缀，后面补seq，假设是11位，则单个tag最多每秒生成2048个id，也就是最大支持2048/秒
 * <p>
 * 为了处理ntp不同步的问题，需要在redis中存储当前的时间戳，每次生成时判断redis中的时间戳和本机时间戳是否匹配
 * 如果本机时间戳>redis时间戳，则使用本机时间戳覆盖
 * 如果本机时间戳<=redis时间戳，则使用redis时间戳
 * <p>
 * 为了提高利用率，可以规定一个起始时间戳
 * <p>
 * seq取11位的情况下（最大2048/秒），用到52位（js的最大精度），可以使用69730年
 * seq取12位的情况下（最大4096/秒），用到52位（js的最大精度），可以使用34865年
 * seq取13位的情况下（最大8192/秒），用到52位（js的最大精度），可以使用17432年
 * <p>
 * Created by caojiajun on 2023/8/9
 */
public class CamelliaStrictIdGen2 {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaStrictIdGen2.class);

    private final CamelliaRedisTemplate template;
    private final String cacheKeyPrefix;
    private final long twepoch;
    private final int cacheExpireSeconds;
    private final int seqBits;
    private final int seqMax;

    public CamelliaStrictIdGen2(CamelliaStrictIdGen2Config config) {
        this.template = config.getRedisTemplate();
        this.cacheKeyPrefix = config.getCacheKeyPrefix();
        this.twepoch = config.getTwepoch();
        this.cacheExpireSeconds = config.getCacheExpireSeconds();
        this.seqBits = config.getSeqBits();
        this.seqMax = 1 << (config.getSeqBits() + 1) - 1;

        try (Jedis jedis = template.getReadJedis(new byte[0])) {
            List<String> time = jedis.time();
            long redisTime = Long.parseLong(time.get(0));
            long localTime = System.currentTimeMillis() / 1000;
            logger.debug("redisTime = {}, localTime = {}, (redisTime-localTime) = {}", redisTime, localTime, (redisTime - localTime));
            if (redisTime - localTime > config.getRedisTimeCheckThresholdSeconds()) {
                logger.warn("redisTime - localTime > 60s, please check your ntp");
                throw new IllegalStateException("redisTime - localTime > 60s, please check your ntp");
            }
        }
    }

    private static final String evalScript = "local seconds = ARGV[2];" +
            "local ctime = redis.call('get', KEYS[1]);" +
            "if ctime == false then " +
            "redis.call('setex', KEYS[1], ARGV[1], ARGV[2]);" +
            "elseif tonumber(ctime) < tonumber(ARGV[2]) then " +
            "redis.call('setex', KEYS[1], ARGV[1], ARGV[2]);" +
            "elseif tonumber(ctime) > tonumber(ARGV[2]) then " +
            "seconds = ctime;" +
            "end " +
            "local ret = redis.call('incr', ARGV[3]..seconds);" +
            "redis.call('expire', ARGV[3]..seconds, ARGV[1]);" +
            "return ret..'|'..seconds;";

    public long genId(String tag) {
        String hashTag = cacheKeyPrefix + "{" + tag + "}";
        while (true) {
            long seconds = now() / 1000;
            String timeKey = CacheUtil.buildCacheKey(hashTag, "time");
            byte[] result = (byte[])template.eval(evalScript, 1, timeKey, String.valueOf(cacheExpireSeconds), String.valueOf(seconds), hashTag);
            String res = new String(result, StandardCharsets.UTF_8);
            String[] split = res.split("\\|");
            int seq = Integer.parseInt(split[0]);
            long realSeconds = Long.parseLong(split[1]);
            if (seq < seqMax) {
                return (realSeconds << seqBits) | seq;
            } else {
                long sleepMs = (realSeconds + 1) * 1000L - now();
                if (sleepMs > 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(sleepMs + 1);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    private long now() {
        return System.currentTimeMillis() - twepoch;
    }
}
