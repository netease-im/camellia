package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.LocalStorageExecutors;
import org.jctools.queues.MpscBlockingConsumerArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by caojiajun on 2025/1/17
 */
public class WalWriteExecutor {

    private static final Logger logger = LoggerFactory.getLogger(WalWriteTask.class);

    private int threads = 0;
    private WalMode walMode;
    private final List<BlockingQueue<WalWriteTask>> queueList = new ArrayList<>();

    private final IWalManifest walManifest;
    private final WalReadWrite walReadWrite;

    public WalWriteExecutor(IWalManifest walManifest, WalReadWrite walReadWrite) {
        this.walManifest = walManifest;
        this.walReadWrite = walReadWrite;
    }

    public void start() {
        walMode = LocalStorageExecutors.getInstance().getWalMode();
        if (walMode == WalMode.async) {
            threads = LocalStorageExecutors.getInstance().getWalThreads();
            for (int i = 0; i < threads; i++) {
                BlockingQueue<WalWriteTask> queue = new MpscBlockingConsumerArrayQueue<>(10240);
                queueList.add(queue);
                new Thread(() -> write0(queue), "wal-flush-" + i).start();
            }
            logger.info("wal write executor start in {}, threads = {}", walMode, threads);
        } else if (walMode == WalMode.sync) {
            logger.info("wal write executor start in {}", walMode);
        } else {
            throw new IllegalArgumentException("illegal wal mode");
        }
    }

    public void submit(short slot, WalWriteTask task) throws Exception {
        if (walMode == WalMode.sync) {
            flush(Collections.singletonList(task));
        } else if (walMode == WalMode.async) {
            int index = slot % threads;
            queueList.get(index).put(task);
        } else {
            throw new IllegalStateException("illegal wal mode");
        }
    }

    private void write0(BlockingQueue<WalWriteTask> queue) {
        List<WalWriteTask> tasks = new ArrayList<>();
        while (true) {
            try {
                {
                    WalWriteTask task = queue.take();
                    tasks.add(task);
                }
                while (true) {
                    WalWriteTask task = queue.poll();
                    if (task == null) {
                        break;
                    }
                    tasks.add(task);
                    if (tasks.size() >= 100) {
                        break;
                    }
                }
                try {
                    flush(tasks);
                } finally {
                    tasks.clear();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void flush(List<WalWriteTask> tasks) {
        boolean success;
        try {
            Map<Long, List<LogRecord>> map = new HashMap<>();
            for (WalWriteTask task : tasks) {
                List<LogRecord> list = map.computeIfAbsent(task.fileId(), k -> new ArrayList<>());
                list.add(task.record());
            }
            for (Map.Entry<Long, List<LogRecord>> entry : map.entrySet()) {
                Long fileId = entry.getKey();
                ReentrantLock lock = walManifest.getWriteLock(fileId);
                lock.lock();
                long offset;
                try {
                    //获取文件已经写到哪里了
                    offset = walManifest.getFileWriteNextOffset(fileId);
                    //批量写入
                    long nextOffset = walReadWrite.write(fileId, offset, entry.getValue());
                    //更新文件已经写到哪里了
                    walManifest.updateFileWriteNextOffset(fileId, nextOffset);
                } finally {
                    lock.unlock();
                }
                Map<Short, SlotWalOffset> slotWalOffsetMap = new HashMap<>();
                for (LogRecord logRecord : entry.getValue()) {
                    slotWalOffsetMap.put(logRecord.getSlot(), new SlotWalOffset(logRecord.getId(), fileId, offset));
                }
                for (Map.Entry<Short, SlotWalOffset> walOffsetEntry : slotWalOffsetMap.entrySet()) {
                    Short slot = walOffsetEntry.getKey();
                    SlotWalOffset slotWalOffset = walOffsetEntry.getValue();
                    //更新slot已经写到哪里了
                    walManifest.updateSlotWalOffsetEnd(slot, slotWalOffset);
                }
            }
            success = true;
        } catch (Exception e) {
            logger.error("flush wal error", e);
            success = false;
        }
        if (success) {
            for (WalWriteTask task : tasks) {
                task.future().complete(WalWriteResult.success);
            }
        } else {
            for (WalWriteTask task : tasks) {
                task.future().complete(WalWriteResult.error);
            }
        }
    }

}
