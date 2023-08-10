package com.netease.nim.camellia.id.gen.strict;

import com.netease.nim.camellia.id.gen.common.CamelliaIdGenConstants;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

/**
 * Created by caojiajun on 2023/8/10
 */
public class CamelliaStrictIdGen2Config {

    private CamelliaRedisTemplate redisTemplate;
    private String cacheKeyPrefix = CamelliaIdGenConstants.Strict2.cacheKeyPrefix;
    private long twepoch = CamelliaIdGenConstants.Strict2.twepoch;
    private int cacheExpireSeconds = CamelliaIdGenConstants.Strict2.cacheExpireSeconds;
    private int seqBits = CamelliaIdGenConstants.Strict2.seqBits;
    private int redisTimeCheckThresholdSeconds = CamelliaIdGenConstants.Strict2.redisTimeCheckThresholdSeconds;

    public CamelliaRedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(CamelliaRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String getCacheKeyPrefix() {
        return cacheKeyPrefix;
    }

    public void setCacheKeyPrefix(String cacheKeyPrefix) {
        this.cacheKeyPrefix = cacheKeyPrefix;
    }

    public long getTwepoch() {
        return twepoch;
    }

    public void setTwepoch(long twepoch) {
        this.twepoch = twepoch;
    }

    public int getCacheExpireSeconds() {
        return cacheExpireSeconds;
    }

    public void setCacheExpireSeconds(int cacheExpireSeconds) {
        this.cacheExpireSeconds = cacheExpireSeconds;
    }

    public int getSeqBits() {
        return seqBits;
    }

    public void setSeqBits(int seqBits) {
        this.seqBits = seqBits;
    }

    public int getRedisTimeCheckThresholdSeconds() {
        return redisTimeCheckThresholdSeconds;
    }

    public void setRedisTimeCheckThresholdSeconds(int redisTimeCheckThresholdSeconds) {
        this.redisTimeCheckThresholdSeconds = redisTimeCheckThresholdSeconds;
    }
}
