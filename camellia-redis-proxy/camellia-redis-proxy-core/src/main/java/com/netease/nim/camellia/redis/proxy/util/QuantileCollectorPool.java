package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by caojiajun on 2022/11/22
 */
public class QuantileCollectorPool {

    private static final LinkedBlockingQueue<QuantileCollector> pool;

    static {
        int initSize = ProxyDynamicConf.getInt("quantile.collector.pool.init.size", 32);
        int maxSize = ProxyDynamicConf.getInt("quantile.collector.pool.max.size", 2048);
        pool = new LinkedBlockingQueue<>(maxSize);
        for (int i=0; i<initSize; i++) {
            pool.offer(new QuantileCollector());
        }
    }

    /**
     * 获取一个QuantileCollector
     * @return QuantileCollector
     */
    public static QuantileCollector borrowQuantileCollector() {
        QuantileCollector collector = pool.poll();
        if (collector == null) {
            collector = new QuantileCollector();
        }
        if (!collector.isInit()) {
            collector.init();
        }
        return collector;
    }

    /**
     * 回收一个QuantileCollector
     * @param collector QuantileCollector
     */
    public static void returnQuantileCollector(QuantileCollector collector) {
        pool.offer(collector);
    }
}
