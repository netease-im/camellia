package com.netease.nim.camellia.hot.key.server.calculate;


import com.netease.nim.camellia.hot.key.server.utils.TimeCache;

/**
 * 基于滑动窗口的热key检测计数器
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyCounter {

    private final long[] buckets;
    private final int bucketSize;
    private final long millisPerBucket;

    private int index;
    private long lastUpdateTime = TimeCache.currentMillis;
    private long total = -1;

    public HotKeyCounter(long checkMillis) {
        //init
        millisPerBucket = 100;
        bucketSize = (int) (checkMillis / millisPerBucket);
        buckets = new long[bucketSize];
        index = 0;
    }

    /**
     * 线程不安全的，上层务必只有单线程调用
     * @param count count
     * @return 当前值
     */
    public long update(long count) {
        int slideStep = (int) ((TimeCache.currentMillis - lastUpdateTime) / millisPerBucket);
        if (slideStep > 0) {
            slideToNextBucket(slideStep);
            total = -1;
        }
        lastUpdateTime = TimeCache.currentMillis;
        buckets[index] += count;
        if (total == -1) {
            long c = 0;
            for (long bucket : buckets) {
                c += bucket;
            }
            total = c;
        } else {
            total += count;
        }
        return total;
    }

    private void slideToNextBucket(int step) {
        if (step >= bucketSize){
            for (int i=0; i<bucketSize; i++) {
                buckets[i] = 0;
            }
            index = 0;
            return;
        }
        if (index + step < bucketSize) {
            for (int i=index+1; i<=index+step; i++) {
                buckets[i] = 0;
            }
            index = index + step;
        } else {
            for (int i=index+1; i<bucketSize; i++) {
                buckets[i] = 0;
            }
            for (int i=0; i<=(index+step-bucketSize); i++) {
                buckets[i] = 0;
            }
            index = index + step - bucketSize;
        }
    }
}
