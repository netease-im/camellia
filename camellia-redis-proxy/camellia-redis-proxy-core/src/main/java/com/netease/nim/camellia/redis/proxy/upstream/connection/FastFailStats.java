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

    /**
     * 是否要快速失败
     * @param url url
     * @return true/false
     */
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

    /**
     * 增加一次失败
     * @param url url
     */
    public void incrFail(String url) {
        AtomicLong failCount = CamelliaMapUtils.computeIfAbsent(failCountMap, url, k -> new AtomicLong());
        failCount.incrementAndGet();
    }

    /**
     * 重置失败
     * @param url url
     */
    public void resetFail(String url) {
        resetFailTimestamp(url);
        resetFailCount(url);
    }

    private long getFailTimestamp(String url) {
        AtomicLong failTimestamp = CamelliaMapUtils.computeIfAbsent(failTimestampMap, url, k -> new AtomicLong(0L));
        return failTimestamp.get();
    }

    private void setFailTimestamp(String url) {
        AtomicLong failTimestamp = CamelliaMapUtils.computeIfAbsent(failTimestampMap, url, k -> new AtomicLong(0L));
        failTimestamp.set(TimeCache.currentMillis);
    }

    private void resetFailTimestamp(String url) {
        AtomicLong failCount = CamelliaMapUtils.computeIfAbsent(failTimestampMap, url, k -> new AtomicLong(0L));
        failCount.set(0L);
    }

    private void resetFailCount(String url) {
        AtomicLong failCount = CamelliaMapUtils.computeIfAbsent(failCountMap, url, k -> new AtomicLong());
        failCount.set(0L);
    }

    private long getFailCount(String url) {
        AtomicLong failCount = CamelliaMapUtils.computeIfAbsent(failCountMap, url, k -> new AtomicLong());
        return failCount.get();
    }

}
