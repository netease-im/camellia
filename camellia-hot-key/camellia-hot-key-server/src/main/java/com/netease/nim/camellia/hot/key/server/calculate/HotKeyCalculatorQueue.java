package com.netease.nim.camellia.hot.key.server.calculate;

import com.netease.nim.camellia.hot.key.common.model.KeyCounter;
import com.netease.nim.camellia.hot.key.server.conf.WorkQueueType;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.MpscLinkedQueue;
import org.jctools.queues.atomic.MpscAtomicArrayQueue;
import org.jctools.queues.atomic.MpscLinkedAtomicQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    public HotKeyCalculatorQueue(WorkQueueType workQueueType, int bizWorkQueueCapacity) {
        this.queue = initQueue(workQueueType, bizWorkQueueCapacity);
        this.id = idGen.getAndIncrement();
    }

    private Queue<List<KeyCounter>> initQueue(WorkQueueType workQueueType, int bizWorkQueueCapacity) {
        if (workQueueType == WorkQueueType.LinkedBlockingQueue) {
            return new LinkedBlockingQueue<>(bizWorkQueueCapacity);
        } else if (workQueueType == WorkQueueType.ArrayBlockingQueue) {
            return new ArrayBlockingQueue<>(bizWorkQueueCapacity);
        } else if (workQueueType == WorkQueueType.ConcurrentLinkedQueue) {
            return new ConcurrentLinkedQueue<>();
        } else if (workQueueType == WorkQueueType.MpscArrayQueue) {
            return new MpscArrayQueue<>(bizWorkQueueCapacity);
        } else if (workQueueType == WorkQueueType.MpscLinkedQueue) {
            return new MpscLinkedQueue<>();
        } else if (workQueueType == WorkQueueType.MpscAtomicArrayQueue) {
            return new MpscAtomicArrayQueue<>(bizWorkQueueCapacity);
        } else if (workQueueType == WorkQueueType.MpscLinkedAtomicQueue) {
            return new MpscLinkedAtomicQueue<>();
        } else {
            return new LinkedBlockingQueue<>(bizWorkQueueCapacity);
        }
    }

    public long getId() {
        return id;
    }

    public void push(List<KeyCounter> counters) {
        boolean success = queue.offer(counters);
        if (!success) {
            logger.error("HotKeyCalculatorQueue full");
        }
    }

    public int pendingSize() {
        return queue.size();
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
        }, "hot-key-calculator-queue-" + id).start();
        logger.info("hot key calculator {} start success", id);
    }
}
