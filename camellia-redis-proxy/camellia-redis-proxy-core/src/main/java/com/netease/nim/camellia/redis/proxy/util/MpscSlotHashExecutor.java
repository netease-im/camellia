package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.tools.executor.CamelliaExecutor;
import com.netease.nim.camellia.tools.executor.CamelliaExecutorMonitor;
import com.netease.nim.camellia.tools.utils.MathUtil;
import io.netty.util.concurrent.FastThreadLocalThread;
import org.jctools.queues.MpscBlockingConsumerArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2024/6/6
 */
public class MpscSlotHashExecutor implements CamelliaExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MpscSlotHashExecutor.class);
    private static final RejectedExecutionHandler defaultRejectedPolicy = new AbortPolicy();

    private final String name;
    private final int poolSize;
    private final boolean poolSizeIs2Power;
    private final AtomicLong workerIdGen = new AtomicLong(1);
    private final List<WorkThread> workThreads;
    private final RejectedExecutionHandler rejectedExecutionHandler;

    private final AtomicLong[] slots = new AtomicLong[RedisClusterCRC16Utils.SLOT_SIZE];

    public MpscSlotHashExecutor(String name, int poolSize, int queueSize) {
        this(name, poolSize, queueSize, defaultRejectedPolicy, null);
    }

    public MpscSlotHashExecutor(String name, int poolSize, int queueSize, RejectedExecutionHandler rejectedExecutionHandler) {
        this(name, poolSize, queueSize, rejectedExecutionHandler, null);
    }

    public MpscSlotHashExecutor(String name, int poolSize, int queueSize, RejectedExecutionHandler rejectedExecutionHandler, Runnable workThreadInitCallback) {
        this.name = CamelliaExecutorMonitor.genExecutorName(name);
        this.workThreads = new ArrayList<>(poolSize);
        for (int i=0; i<poolSize; i++) {
            WorkThread workThread = new WorkThread(this, queueSize, workThreadInitCallback);
            workThread.start();
            this.workThreads.add(workThread);
        }
        this.poolSize = poolSize;
        this.poolSizeIs2Power = MathUtil.is2Power(poolSize);
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        for (int i=0; i<slots.length; i++) {
            slots[i] = new AtomicLong();
        }
        logger.info("MpscSlotHashExecutor start success, name = {}, poolSize = {}, queueSize = {}", name, poolSize, queueSize);
    }

    /**
     * 根据hashKey选取一个固定的工作线程执行一个任务
     * @param slot slot
     * @param runnable 无返回结果的任务
     * @return 任务结果
     */
    public Future<Void> submit(int slot, Runnable runnable) {
        int index = MathUtil.mod(poolSizeIs2Power, slot, poolSize);
        SlotTask task = new SlotTask(slot, runnable);
        AtomicLong slotCounter = slots[slot];
        slotCounter.incrementAndGet();
        boolean success = workThreads.get(index).submit(task);
        if (!success) {
            try {
                rejectedExecutionHandler.rejectedExecution(task, this);
            } finally {
                slotCounter.decrementAndGet();
            }
        }
        return task;
    }

    public boolean canRunToCompletion(int slot) {
        return slots[slot].get() == 0;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * 获取等待队列的大小
     * @return 大小
     */
    public int getQueueSize() {
        int queueSize = 0;
        for (WorkThread workThread : workThreads) {
            queueSize += workThread.queueSize();
        }
        return queueSize;
    }

    public static interface RejectedExecutionHandler {
        void rejectedExecution(Runnable runnable, MpscSlotHashExecutor executor);
    }

    public static class AbortPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, MpscSlotHashExecutor executor) {
            throw new RejectedExecutionException("Task " + runnable.toString() + " rejected from " + executor.getName());
        }
    }

    public static class DiscardPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, MpscSlotHashExecutor executor) {
            logger.warn("Task " + runnable.toString() + " is discard from " + executor.getName());
        }
    }

    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, MpscSlotHashExecutor executor) {
            runnable.run();
        }
    }

    private static class SlotTask extends FutureTask<Void> {

        private final int slot;

        public SlotTask(int slot, Runnable runnable) {
            super(runnable, null);
            this.slot = slot;
        }

        public int getSlot() {
            return slot;
        }
    }

    private static class WorkThread extends FastThreadLocalThread {

        private final BlockingQueue<SlotTask> queue;
        private final Runnable initCallback;
        private final MpscSlotHashExecutor executor;

        public WorkThread(MpscSlotHashExecutor executor, int queueSize, Runnable initCallback) {
            this.executor = executor;
            this.queue = new MpscBlockingConsumerArrayQueue<>(queueSize);
            setName("mpsc-slot-hash-executor-" + executor.getName() + "-" + executor.workerIdGen.getAndIncrement());
            this.initCallback = initCallback;
        }

        public boolean submit(SlotTask task) {
            return queue.offer(task);
        }

        public int queueSize() {
            return queue.size();
        }

        @Override
        public void run() {
            if (initCallback != null) {
                initCallback.run();
            }
            while (true) {
                try {
                    SlotTask task = queue.poll(1, TimeUnit.SECONDS);
                    if (task != null) {
                        try {
                            task.run();
                        } finally {
                            executor.slots[task.getSlot()].decrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    logger.error("MpscSlotHashExecutor execute task error, name = {}", executor.name, e);
                }
            }
        }
    }
}
