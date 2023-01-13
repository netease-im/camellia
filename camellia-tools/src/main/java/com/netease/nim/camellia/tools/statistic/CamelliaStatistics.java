package com.netease.nim.camellia.tools.statistic;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 * Created by caojiajun on 2022/7/21
 */
public class CamelliaStatistics {

    private final LongAdder count = new LongAdder();
    private final LongAdder sum = new LongAdder();
    private final MaxValue maxValue = new MaxValue();

    /**
     * 0-100 1ms 100-buckets total-101-buckets
     * 101-200 5ms 20-buckets total-121-buckets
     * 201-500 10ms 30-buckets total-151-buckets
     * 501-3000 50ms 50-buckets total-201-buckets
     * 3001-10000 200ms 35-buckets total-236-buckets
     * 10001-30000 1000ms 20-buckets total-256-buckets
     * total-256-buckets
     */
    private final AtomicLong[] distribute;
    private final boolean exact;

    public CamelliaStatistics(boolean exact, int expectMaxValue) {
        this.exact = exact;
        if (exact) {
            this.distribute = new AtomicLong[expectMaxValue];
        } else {
            this.distribute = new AtomicLong[256];
        }
        for (int i=0; i<distribute.length; i++) {
            distribute[i] = new AtomicLong(0);
        }
    }

    public CamelliaStatistics() {
        this(false, -1);
    }

    public void update(long value) {
        count.increment();
        sum.add(value);
        maxValue.update(value);
        if (value < 0) return;
        if (exact) {
            AtomicLong distributeCounter;
            if (value >= distribute.length) {
                distributeCounter = distribute[distribute.length - 1];
            } else {
                distributeCounter = distribute[(int) value];
            }
            if (distributeCounter != null) {
                distributeCounter.incrementAndGet();
            }
        } else {
            AtomicLong distributeCounter;
            if (value <= 100) {
                distributeCounter = distribute[(int) value];
            } else if (value <= 200) {
                distributeCounter = distribute[(int) (101 + (value - 101) / 5)];
            } else if (value <= 500) {
                distributeCounter = distribute[(int) (121 + (value - 201) / 10)];
            } else if (value <= 3000) {
                distributeCounter = distribute[(int) (151 + (value - 501) / 50)];
            } else if (value <= 10000) {
                distributeCounter = distribute[(int) (201 + (value - 3001) / 200)];
            } else if (value <= 30000) {
                distributeCounter = distribute[(int) (236 + (value - 10001) / 1000)];
            } else {
                distributeCounter = distribute[255];
            }
            if (distributeCounter != null) {
                distributeCounter.incrementAndGet();
            }
        }
    }

    public CamelliaStatsData getStatsDataAndReset() {
        long sum = this.sum.sumThenReset();
        long count = this.count.sumThenReset();
        long max = this.maxValue.getAndSet(0);
        double avg;
        if (count == 0) {
            avg = 0.0;
        } else {
            avg = (double) sum / count;
        }

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
        if (exact) {
            long c = 0;
            long lastIndexNum = distribute[distribute.length - 1].get();
            for (int i = 0; i < distribute.length; i++) {
                c += distribute[i].getAndSet(0);
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
            return new CamelliaStatsData(count, avg, max, sum, p50, p75, p90, p95, p99, p999);
        } else {
            long c = 0;
            for (int i=0; i < distribute.length; i++) {
                long current = distribute[i].getAndSet(0);
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
            return new CamelliaStatsData(count, avg, max, sum, p50, p75, p90, p95, p99, p999);
        }
    }

    private long index2Real(int index, long current, long offset, long max) {
        if (index >= 0 && index <= 100) {
            return index;
        } else if (index <= 121) {
            double rate = offset / (current*1.0);
            return 100 + (index - 100 + 1) * 5L + (long)(5 * rate);
        } else if (index <= 151) {
            double rate = offset / (current*1.0);
            return 200 + (index - 121 + 1) * 10L + (long)(10 * rate);
        } else if (index <= 201) {
            double rate = offset / (current*1.0);
            return 500 + (index - 151 + 1) * 50L + (long)(50 * rate);
        } else if (index <= 236) {
            double rate = offset / (current*1.0);
            return 3000 + (index - 201 + 1) * 200L + (long)(200 * rate);
        } else if (index < 255) {
            double rate = offset / (current*1.0);
            return 3000 + (index - 236 + 1) * 1000L + (long)(1000 * rate);
        } else if (index == 255) {
            return 30000 + (long)(offset / (current*1.0) * (max - 30000));
        }
        return 0;
    }

    private long quantileExceed(long max, int maxIndex, long lastIndexNum, long count, long quantilePosition) {
        return Math.round(max - ((max - maxIndex + 1) / (lastIndexNum * 1.0)) * (count - quantilePosition));
    }

    public CamelliaStatsData getStatsData() {
        long sum = this.sum.sum();
        long count = this.count.sum();
        long max = maxValue.get();
        double avg = (double) sum / count;
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
        long c = 0;
        if (exact) {
            long lastIndexNum = distribute[distribute.length - 1].get();
            for (int i = 0; i < distribute.length; i++) {
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
        } else {
            for (int i=0; i < distribute.length; i++) {
                long current = distribute[i].get();
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
        }
        return new CamelliaStatsData(count, avg, max, sum, p50, p75, p90, p95, p99, p999);
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
