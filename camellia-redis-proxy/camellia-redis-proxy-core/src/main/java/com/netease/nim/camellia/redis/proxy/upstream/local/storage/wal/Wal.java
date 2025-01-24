package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;


import com.netease.nim.camellia.redis.proxy.monitor.LocalStorageMonitor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec.WalEntryCodec;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.LocalStorageReadWrite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2025/1/14
 */
public class Wal {

    private static final Logger logger = LoggerFactory.getLogger(Wal.class);

    private final LocalStorageReadWrite readWrite;
    private final LogRecordIdGen idGen = new LogRecordIdGen();

    private final WalReadWrite walReadWrite;
    private final IWalManifest walManifest;

    private final WalWriteExecutor executor;

    public Wal(LocalStorageReadWrite readWrite) {
        this.readWrite = readWrite;
        this.walManifest = readWrite.getWalManifest();
        this.walReadWrite = readWrite.getWalReadWrite();
        this.executor = new WalWriteExecutor(walManifest, walReadWrite);
        this.executor.start();
    }

    /**
     * 追加一条wal日志
     * @param slot 所在slot
     * @param walEntry log条目
     * @return 写入结果
     * @throws Exception 异常
     */
    public CompletableFuture<WalWriteResult> append(short slot, WalEntry walEntry) throws Exception {
        long startTime = System.nanoTime();
        //获取log_record_id
        long recordId = idGen.nextId();
        //构造record
        byte[] data = WalEntryCodec.encode(walEntry);
        LogRecord record = new LogRecord(recordId, slot, data);
        //获取wal_file_id
        long fileId = walManifest.fileId(slot);
        //异步批量落盘
        CompletableFuture<WalWriteResult> future = new CompletableFuture<>();
        executor.submit(slot, new WalWriteTask(record, fileId, future));
        future.thenAccept(result -> LocalStorageMonitor.time("wal_append", System.nanoTime() - startTime));
        return future;
    }

    /**
     * 获取slot当前写到哪里了
     * @param slot slot
     * @return SlotWalOffset
     */
    public SlotWalOffset getSlotWalOffsetEnd(short slot) {
        return walManifest.getSlotWalOffsetEnd(slot);
    }

    /**
     * flush slot下的SlotWalOffset，则SlotWalOffset之前关于指定slot的wal日志条目都无效了
     * @param slot slot
     * @param offset SlotWalOffset
     */
    public void flush(short slot, SlotWalOffset offset) {
        if (offset == null) {
            return;
        }
        walManifest.updateSlotWalOffsetStart(slot, offset);
    }

    /**
     * recover from wal
     * @throws Exception exception
     */
    public void recover() throws Exception {
        logger.info("recover from wal start.");
        long start = System.currentTimeMillis();
        try {
            Map<Short, SlotWalOffset> walOffsetMap = walManifest.getSlotWalOffsetStartMap();
            Map<Long, Long> fileIdOffsetMap = new TreeMap<>(Comparator.comparingLong(o -> o));
            for (Map.Entry<Short, SlotWalOffset> entry : walOffsetMap.entrySet()) {
                long fileId = entry.getValue().fileId();
                SlotWalOffset slotWalOffset = entry.getValue();
                Long offset = fileIdOffsetMap.get(fileId);
                if (offset == null || slotWalOffset.fileOffset() < offset) {
                    fileIdOffsetMap.put(fileId, slotWalOffset.fileOffset());
                }
            }
            for (Map.Entry<Long, Long> entry : fileIdOffsetMap.entrySet()) {
                Long fileId = entry.getKey();
                Long offset = entry.getValue();
                recover(fileId, offset, walOffsetMap);
            }
            logger.info("recover from wal success, spendMs = {}", System.currentTimeMillis() - start);
        } catch (Exception e) {
            logger.error("recover from wal error, spendMs = {}", System.currentTimeMillis() - start);
            throw e;
        }
    }

    private void recover(long fileId, long offset, Map<Short, SlotWalOffset> walOffsetMap) throws Exception {
        while (true) {
            WalReadResult result = walReadWrite.read(fileId, offset);
            if (result == null) {
                return;
            }
            for (LogRecord record : result.records()) {
                short slot = record.getSlot();
                if (record.getId() <= walOffsetMap.get(slot).recordId()) {
                    continue;
                }
                byte[] data = record.getData();
                WalEntry walEntry = WalEntryCodec.decode(data);
                walEntry.recover(slot, readWrite);
            }
            offset = result.nextOffset();
        }
    }
}
