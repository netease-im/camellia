package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

import com.netease.nim.camellia.tools.utils.SysUtils;
import org.jctools.queues.MpscBlockingConsumerArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * Created by caojiajun on 2025/1/17
 */
public class WalWriteExecutor {

    private static final Logger logger = LoggerFactory.getLogger(WalWriteTask.class);

    private int threads = 0;
    private final List<BlockingQueue<WalWriteTask>> queueList = new ArrayList<>();

    private final IWalManifest walManifest;
    private final WalReadWrite walReadWrite;

    public WalWriteExecutor(IWalManifest walManifest, WalReadWrite walReadWrite) {
        this.walManifest = walManifest;
        this.walReadWrite = walReadWrite;
    }

    public void start() {
        threads = SysUtils.getCpuNum();
        for (int i=0; i<threads; i++) {
            BlockingQueue<WalWriteTask> queue = new MpscBlockingConsumerArrayQueue<>(10240);
            queueList.add(queue);
            new Thread(() -> write0(queue), "wal-flush-" + i).start();
        }
        logger.info("wal write executor start, threads = {}", threads);
    }

    public void submit(short slot, WalWriteTask task) throws Exception {
        int index = slot % threads;
        queueList.get(index).put(task);
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
                //获取文件已经写到哪里了
                long offset = walManifest.getFileWriteNextOffset(fileId);
                //批量写入
                long nextOffset = walReadWrite.write(fileId, offset, entry.getValue());
                //更新文件已经写到哪里了
                walManifest.updateFileWriteNextOffset(fileId, nextOffset);
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
