package com.netease.nim.camellia.redis.proxy.upstream.connection;

import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2023/1/31
 */
public class FastFailStats {

    private final ConcurrentHashMap<String, AtomicLong> failCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> failTimestampMap = new ConcurrentHashMap<>();

    private int failCountThreshold;
    private long failBanMillis;

    public FastFailStats(int failCountThreshold, long failBanMillis) {
        this.failCountThreshold = failCountThreshold;
        this.failBanMillis = failBanMillis;
    }

    public int getFailCountThreshold() {
        return failCountThreshold;
    }

    public long getFailBanMillis() {
        return failBanMillis;
    }

    public void setFailCountThreshold(int failCountThreshold) {
        this.failCountThreshold = failCountThreshold;
    }

    public void setFailBanMillis(long failBanMillis) {
        this.failBanMillis = failBanMillis;
    }

    public boolean fastFail(String url) {
        //如果client处于不可用状态，检查不可用时长
        long failTimestamp = getFailTimestamp(url);
        if (TimeCache.currentMillis - failTimestamp < failBanMillis) {
            //如果错误时间戳在禁用时间范围内，则直接返回null
            //此时去重置一下计数器，这样确保failBanMillis到期之后failCount从0开始计算
            resetFailCount(url);
            String log = "currentTimeMillis - failTimestamp < failBanMillis[" + failBanMillis + "], immediate return null, key = " + url;
            ErrorLogCollector.collect(RedisConnectionHub.class, log);
            return true;
        }
        long failCount = getFailCount(url);
        if (failCount > failCountThreshold) {
            //如果错误次数超过了阈值，则设置当前时间为错误时间戳，并重置计数器
            //接下来的failBanMillis时间内，都会直接返回null
            setFailTimestamp(url);
            resetFailCount(url);
            String log = "failCount > failCountThreshold[" + failCountThreshold + "], immediate return null, key = " + url;
            ErrorLogCollector.collect(RedisConnectionHub.class, log);
            return true;
        }
        return false;
    }

    public long getFailTimestamp(String key) {
        AtomicLong failTimestamp = CamelliaMapUtils.computeIfAbsent(failTimestampMap, key, k -> new AtomicLong(0L));
        return failTimestamp.get();
    }

    public void setFailTimestamp(String key) {
        AtomicLong failTimestamp = CamelliaMapUtils.computeIfAbsent(failTimestampMap, key, k -> new AtomicLong(0L));
        failTimestamp.set(TimeCache.currentMillis);
    }

    public void resetFailTimestamp(String key) {
        AtomicLong failCount = CamelliaMapUtils.computeIfAbsent(failTimestampMap, key, k -> new AtomicLong(0L));
        failCount.set(0L);
    }

    public void resetFailCount(String key) {
        AtomicLong failCount = CamelliaMapUtils.computeIfAbsent(failCountMap, key, k -> new AtomicLong());
        failCount.set(0L);
    }

    public long getFailCount(String key) {
        AtomicLong failCount = CamelliaMapUtils.computeIfAbsent(failCountMap, key, k -> new AtomicLong());
        return failCount.get();
    }

    public void incrFail(String key) {
        AtomicLong failCount = CamelliaMapUtils.computeIfAbsent(failCountMap, key, k -> new AtomicLong());
        failCount.incrementAndGet();
    }

    public void resetFail(String key) {
        resetFailTimestamp(key);
        resetFailCount(key);
    }
}
