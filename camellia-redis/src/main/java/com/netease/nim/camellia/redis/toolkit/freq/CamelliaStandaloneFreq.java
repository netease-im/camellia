package com.netease.nim.camellia.redis.toolkit.freq;

import com.alibaba.fastjson.JSONObject;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2022/8/1
 */
public class CamelliaStandaloneFreq {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaStandaloneFreq.class);
    private final ConcurrentLinkedHashMap<String, Counter> cache;

    public CamelliaStandaloneFreq(int capacity) {
        cache = new ConcurrentLinkedHashMap.Builder<String, Counter>()
                .initialCapacity(capacity).maximumWeightedCapacity(capacity).build();
    }

    public CamelliaStandaloneFreq() {
        this(100000);
    }

    public CamelliaFreqResponse checkFreqPass(String freqKey, CamelliaFreqConfig freqConfig) {
        return checkFreqPass(freqKey, 1, freqConfig);
    }

    public CamelliaFreqResponse checkFreqPass(String freqKey, int delta, CamelliaFreqConfig freqConfig) {
        try {
            Counter counter = cache.get(freqKey);
            if (counter != null && counter.isExpire()) {
                cache.remove(freqKey);
                counter = null;
            }
            if (counter == null) {
                counter = new Counter();
                Counter oldCounter = cache.putIfAbsent(freqKey, counter);
                if (oldCounter != null) {
                    counter = oldCounter;
                }
            }
            long current = counter.addAndGet(delta);
            if (current == delta) {
                counter.expireTime = System.currentTimeMillis() + freqConfig.getCheckTime();
            }
            boolean pass = current <= freqConfig.getThreshold();
            if (!pass) {
                if (freqConfig.getBanTime() > 0) {
                    if (freqConfig.isDelayBanEnable()) {
                        counter.expireTime = System.currentTimeMillis() + freqConfig.getBanTime();
                    } else {
                        if (current <= freqConfig.getThreshold() + delta) {
                            counter.expireTime = System.currentTimeMillis() + freqConfig.getBanTime();
                        }
                    }
                }
            }
            return new CamelliaFreqResponse(pass, current, CamelliaFreqType.STANDALONE);
        } catch (Throwable e) {
            logger.error("checkFreqPass error, return pass, freqKey = {}, delta = {}, freqConfig = {}", freqKey, delta, JSONObject.toJSONString(freqConfig), e);
            return CamelliaFreqResponse.DEFAULT_PASS;
        }
    }

    private static class Counter {
        private final AtomicLong count = new AtomicLong();
        private long expireTime;

        public boolean isExpire() {
            return System.currentTimeMillis() > expireTime;
        }

        public long addAndGet(int delta) {
            return count.addAndGet(delta);
        }
    }
}
