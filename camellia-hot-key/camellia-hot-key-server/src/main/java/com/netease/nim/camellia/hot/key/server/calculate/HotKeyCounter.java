package com.netease.nim.camellia.hot.key.server.calculate;


import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.server.utils.TimeCache;

import java.util.HashSet;
import java.util.Set;

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
    private Set<String> sourceSet;

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
     * @param source source
     * @return 当前值
     */
    public long update(long count, String source) {
        int slideStep = (int) ((TimeCache.currentMillis - lastUpdateTime) / millisPerBucket);
        if (slideStep > 0) {
            slideToNextBucket(slideStep);
            total = -1;
            lastUpdateTime = TimeCache.currentMillis;
        }
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
        if (source != null) {
            if (sourceSet == null) {
                sourceSet = new HashSet<>();
            }
            if (sourceSet.size() >= HotKeyConstants.Server.maxHotKeySourceSetSize) {
                sourceSet.clear();
            }
            sourceSet.add(source);
        }
        return total;
    }

    /**
     * 获取来源
     * @return source set
     */
    public Set<String> getSourceSet() {
        return sourceSet;
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
