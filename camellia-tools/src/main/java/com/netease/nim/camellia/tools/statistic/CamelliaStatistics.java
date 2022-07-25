package com.netease.nim.camellia.tools.statistic;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2022/7/21
 */
public class CamelliaStatistics {

    private final LongAdder count = new LongAdder();
    private final LongAdder sum = new LongAdder();
    private final MaxValue maxValue = new MaxValue();

    public void update(long value) {
        count.increment();
        sum.add(value);
        maxValue.update(value);
    }

    public CamelliaStatsData getStatsDataAndReset() {
        long sum = this.sum.sumThenReset();
        long count = this.count.sumThenReset();
        long max = this.maxValue.getAndSet(0);
        double avg = (double) sum / count;
        return new CamelliaStatsData(count, avg, max, sum);
    }

    public CamelliaStatsData getStatsData() {
        long sum = this.sum.sum();
        long count = this.count.sum();
        long max = maxValue.get();
        double avg = (double) sum / count;
        return new CamelliaStatsData(count, avg, max, sum);
    }

    private static class MaxValue {
        private final AtomicLong max = new AtomicLong(0L);

        public void update(long value) {
            while (true) {
                long oldValue = max.get();
                if (value > oldValue) {
                    if (max.compareAndSet(oldValue, value)) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }

        public long getAndSet(long newValue) {
            return max.getAndSet(newValue);
        }

        public long get() {
            return max.get();
        }
    }
}
