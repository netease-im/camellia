package com.netease.nim.camellia.hot.key.server;

import com.netease.nim.camellia.hot.key.common.model.KeyCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyCalculatorQueue {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyCalculatorQueue.class);

    private static final AtomicLong idGen = new AtomicLong(0);
    private final Queue<List<KeyCounter>> queue;
    private final long id;

    public HotKeyCalculatorQueue(int bizWorkQueueCapacity) {
        this.queue = new LinkedBlockingQueue<>(bizWorkQueueCapacity);
        this.id = idGen.getAndIncrement();
    }

    public void push(List<KeyCounter> counters) {
        boolean success = queue.offer(counters);
        if (!success) {
            logger.error("HotKeyCalculatorQueue full");
        }
    }

    public void start(HotKeyCalculator calculator) {
        new Thread(() -> {
            while (true) {
                try {
                    List<KeyCounter> counters = queue.poll();
                    if (counters == null) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(10);
                        } catch (InterruptedException e) {
                            logger.error(e.getMessage(), e);
                        }
                        continue;
                    }
                    calculator.calculate(counters);
                } catch (Exception e) {
                    logger.error("hot key calculate error", e);
                }
            }
        }, "hot-key-calculator-" + id).start();
        logger.info("hot key calculator {} start success", id);
    }
}
