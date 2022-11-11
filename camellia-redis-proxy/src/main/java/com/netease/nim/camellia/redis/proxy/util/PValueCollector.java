package com.netease.nim.camellia.redis.proxy.util;

import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2022/11/11
 */
public class PValueCollector {

    private final int expectedMaxValue;
    private LongAdder[] distribute;

    public PValueCollector(int expectedMaxValue) {
        this.expectedMaxValue = expectedMaxValue;
        init();
    }

    private void init() {
        distribute = new LongAdder[expectedMaxValue];
        for (int i=0; i<expectedMaxValue; i++) {
            distribute[i] = new LongAdder();
        }
    }

    public void update(int value) {
        if (value < 0) return;
        LongAdder distributeCounter;
        if (value >= distribute.length) {
            distributeCounter = distribute[distribute.length - 1];
        } else {
            distributeCounter = distribute[value];
        }
        if (distributeCounter != null) {
            distributeCounter.increment();
        }
    }

    public PValue getPValueAndReset() {
        LongAdder[] distribute = this.distribute;
        init();
        long count = 0;
        for (LongAdder adder : distribute) {
            count += adder.sum();
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
        for (int i=0; i < distribute.length; i++) {
            c += distribute[i].sumThenReset();
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
        return new PValue(p50, p75, p90, p95, p99, p999);
    }

    public static class PValue {
        private final long p50;
        private final long p75;
        private final long p90;
        private final long p95;
        private final long p99;
        private final long p999;

        public PValue(long p50, long p75, long p90, long p95, long p99, long p999) {
            this.p50 = p50;
            this.p75 = p75;
            this.p90 = p90;
            this.p95 = p95;
            this.p99 = p99;
            this.p999 = p999;
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
    }
}
