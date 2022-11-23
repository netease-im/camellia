package com.netease.nim.camellia.redis.proxy.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * 0.00-10.00ms 0.01ms 1001-buckets total-1001-buckets
 * 10.01-50.00ms 0.05ms 800-buckets total-1801-buckets
 * 50.01-100.00ms 0.1ms 500-buckets total-2301-buckets
 * 100.01-500.00ms 1ms 400-buckets total-2701-buckets
 * 500.01-2000.00ms 50ms 30-buckets total-2731-buckets
 * 2000.01-10000.00ms 200ms 40-buckets total-2771-buckets
 * total 2771-buckets
 * Created by caojiajun on 2022/11/11
 */
public class QuantileCollector {

    private static final int CAPACITY = 2771;

    private final AtomicBoolean initOk = new AtomicBoolean(false);
    private LongAdder[] distribute;

    private final MaxValue maxValue = new MaxValue();

    public QuantileCollector() {
    }

    public boolean isInit() {
        return initOk.get();
    }

    public void init() {
        if (initOk.compareAndSet(false, true)) {
            distribute = new LongAdder[CAPACITY];
            for (int i = 0; i < CAPACITY; i++) {
                distribute[i] = new LongAdder();
            }
        }
    }

    public void update(int value) {
        if (value < 0) return;
        maxValue.update(value);
        LongAdder distributeCounter;
        if (value <= 1000) {//0.00-10.00
            distributeCounter = distribute[value];
        } else if (value <= 5000) {//10.01-50.00
            distributeCounter = distribute[1001 + (value - 1001) / 5];
        } else if (value <= 10000) {//50.01-100.00
            distributeCounter = distribute[1801 + (value - 5001) / 10];
        } else if (value <= 50000) {
            distributeCounter = distribute[2301 + (value - 10001) / 100];
        } else if (value <= 200000) {
            distributeCounter = distribute[2701 + (value - 50001) / 5000];
        } else if (value <= 1000000) {
            distributeCounter = distribute[2731 + (value - 200001) / 20000];
        } else {
            distributeCounter = distribute[CAPACITY - 1];
        }
        if (distributeCounter != null) {
            distributeCounter.increment();
        }
    }

    public void reset() {
        this.maxValue.getAndSet(0);
        for (LongAdder adder : distribute) {
            adder.reset();
        }
    }

    public QuantileValue getQuantileValueAndReset() {
        long max = this.maxValue.getAndSet(0);
        long[] tmp = new long[distribute.length];
        long count = 0;
        for (int i=0; i < distribute.length; i++) {
            LongAdder adder = distribute[i];
            long sum = adder.sumThenReset();
            tmp[i] = sum;
            count += sum;
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
        for (int i=0; i < tmp.length; i++) {
            long current = tmp[i];
            c += current;
            if (p50 == -1 && c >= p50Position) {
                long offset = current - (c - p50Position);
                p50 = index2Real(i, current, offset, max);
            }
            if (p75 == -1 && c >= p75Position) {
                long offset = current - (c - p75Position);
                p75 = index2Real(i, current, offset, max);;
            }
            if (p90 == -1 && c >= p90Position) {
                long offset = current - (c - p90Position);
                p90 = index2Real(i, current, offset, max);;
            }
            if (p95 == -1 && c >= p95Position) {
                long offset = current - (c - p95Position);
                p95 = index2Real(i, current, offset, max);;
            }
            if (p99 == -1 && c >= p99Position) {
                long offset = current - (c - p99Position);
                p99 = index2Real(i, current, offset, max);;
            }
            if (p999 == -1 && c >= p999Position) {
                long offset = current - (c - p999Position);
                p999 = index2Real(i, current, offset, max);;
            }
        }
        return new QuantileValue(p50, p75, p90, p95, p99, p999, max);
    }

    private long index2Real(int index, long current, long offset, long max) {
        if (index >= 0 && index <= 1000) {
            return index;
        } else if (index <= 1801) {
            double rate = offset / (current*1.0);
            return 1000 + (index - 1000 + 1) * 5L + (long)(5 * rate);
        } else if (index <= 2301) {
            double rate = offset / (current*1.0);
            return 5000 + (index - 1801 + 1) * 10L + (long)(10 * rate);
        } else if (index <= 2701) {
            double rate = offset / (current*1.0);
            return 10000 + (index - 2301 + 1) * 100L + (long)(100 * rate);
        } else if (index <= 2731) {
            double rate = offset / (current*1.0);
            return 50000 + (index - 2701 + 1) * 5000L + (long)(5000 * rate);
        } else if (index < 2770) {
            double rate = offset / (current*1.0);
            return 200000 + (index - 2731 + 1) * 20000L + (long)(20000 * rate);
        } else if (index == 2770) {
            return 1000000L + (long)(offset / (current*1.0) * (max - 1000000L));
        }
        return 0;
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
