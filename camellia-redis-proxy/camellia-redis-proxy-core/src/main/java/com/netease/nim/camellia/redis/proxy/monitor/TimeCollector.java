package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.TimeStats;
import com.netease.nim.camellia.redis.proxy.util.QuantileCollector;
import com.netease.nim.camellia.redis.proxy.util.QuantileCollectorPool;

import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2025/2/8
 */
public class TimeCollector {

    private final LongAdder sum = new LongAdder();
    private final LongAdder count = new LongAdder();
    private final QuantileCollector collector = QuantileCollectorPool.borrowQuantileCollector();

    public void update(long time) {
        sum.add(time);
        count.increment();
        collector.update((int)(time / 10000));
    }

    public TimeStats getStats() {
        TimeStats timeStats = new TimeStats();
        long c = count.sumThenReset();
        long s = sum.sumThenReset();
        timeStats.setCount(c);
        timeStats.setAvgSpendMs((double) s / (1000000.0 * c));
        QuantileCollector.QuantileValue quantileValue = collector.getQuantileValueAndReset();
        timeStats.setSpendMsP50(quantileValue.getP50() / 100.0);
        timeStats.setSpendMsP75(quantileValue.getP75() / 100.0);
        timeStats.setSpendMsP90(quantileValue.getP90() / 100.0);
        timeStats.setSpendMsP95(quantileValue.getP95() / 100.0);
        timeStats.setSpendMsP99(quantileValue.getP99() / 100.0);
        timeStats.setSpendMsP999(quantileValue.getP999() / 100.0);
        timeStats.setMaxSpendMs(quantileValue.getMax() / 100.0);
        QuantileCollectorPool.returnQuantileCollector(collector);
        return timeStats;
    }
}
