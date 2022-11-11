package com.netease.nim.camellia.redis.proxy.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2022/11/11
 */
public class QuantileCollector {

    private final int expectedMaxValue;
    private AtomicLong[] distribute;//已经分散了，AtomicLong和LongAdder性能区别应该不大，但是可以省很多内存
    private final MaxValue maxValue = new MaxValue();

    public QuantileCollector(int expectedMaxValue) {
        this.expectedMaxValue = expectedMaxValue;
        init();
    }

    private void init() {
        distribute = new AtomicLong[expectedMaxValue];
        for (int i=0; i<expectedMaxValue; i++) {
            distribute[i] = new AtomicLong(0);
        }
    }

    public void update(int value) {
        if (value < 0) return;
        maxValue.update(value);
        AtomicLong distributeCounter;
        if (value >= distribute.length) {
            distributeCounter = distribute[distribute.length - 1];
        } else {
            distributeCounter = distribute[value];
        }
        if (distributeCounter != null) {
            distributeCounter.incrementAndGet();
        }
    }

    public QuantileValue getQuantileValueAndReset() {
        AtomicLong[] distribute = this.distribute;
        long max = this.maxValue.getAndSet(0);
        init();
        long count = 0;
        for (AtomicLong adder : distribute) {
            count += adder.get();
        }
        long c = 0;
        long p50Position = (long) (count * 0.5);
        long p75Position = (long) (count * 0.75);
        long p90Position = (long) (count * 0.90);
        long p95Position = (long) (count * 0.95);
        long p99Position = (long) (count * 0.99);
        long p999Position = (long) (count * 0.999);
        long p50 = -1;
        long p75 = -1;
        long p90 = -1;
        long p95 = -1;
        long p99 = -1;
        long p999 = -1;
        long lastIndexNum = distribute[distribute.length - 1].get();
        for (int i=0; i < distribute.length; i++) {
            c += distribute[i].get();
            if (p50 == -1 && c >= p50Position) {
                p50 = i;
            }
            if (p75 == -1 && c >= p75Position) {
                p75 = i;
            }
            if (p90 == -1 && c >= p90Position) {
                p90 = i;
            }
            if (p95 == -1 && c >= p95Position) {
                p95 = i;
            }
            if (p99 == -1 && c >= p99Position) {
                p99 = i;
            }
            if (p999 == -1 && c >= p999Position) {
                p999 = i;
            }
        }
        if (p50 == distribute.length - 1) {
            p50 = quantileExceed(max, distribute.length - 1, lastIndexNum, count, p50Position);
        }
        if (p75 == distribute.length - 1) {
            p75 = quantileExceed(max, distribute.length - 1, lastIndexNum, count, p75Position);
        }
        if (p90 == distribute.length - 1) {
            p90 = quantileExceed(max, distribute.length - 1, lastIndexNum, count, p90Position);
        }
        if (p95 == distribute.length - 1) {
            p95 = quantileExceed(max, distribute.length - 1, lastIndexNum, count, p95Position);
        }
        if (p99 == distribute.length - 1) {
            p99 = quantileExceed(max, distribute.length - 1, lastIndexNum, count, p99Position);
        }
        if (p999 == distribute.length - 1) {
            p999 = quantileExceed(max, distribute.length - 1, lastIndexNum, count, p999Position);
        }
        return new QuantileValue(p50, p75, p90, p95, p99, p999, max);
    }

    private long quantileExceed(long max, int maxIndex, long lastIndexNum, long count, long quantilePosition) {
        return Math.round(max - ((max - maxIndex + 1) / (lastIndexNum * 1.0)) * (count - quantilePosition));
    }

    public static class QuantileValue {
        private final long p50;
        private final long p75;
        private final long p90;
        private final long p95;
        private final long p99;
        private final long p999;
        private final long max;

        public QuantileValue(long p50, long p75, long p90, long p95, long p99, long p999, long max) {
            this.p50 = p50;
            this.p75 = p75;
            this.p90 = p90;
            this.p95 = p95;
            this.p99 = p99;
            this.p999 = p999;
            this.max = max;
        }

        public long getP50() {
            return p50;
        }

        public long getP75() {
            return p75;
        }

        public long getP90() {
            return p90;
        }

        public long getP95() {
            return p95;
        }

        public long getP99() {
            return p99;
        }

        public long getP999() {
            return p999;
        }

        public long getMax() {
            return max;
        }
    }
}
