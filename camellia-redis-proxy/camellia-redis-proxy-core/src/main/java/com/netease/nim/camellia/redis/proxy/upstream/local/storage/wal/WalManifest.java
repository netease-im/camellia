package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.LocalStorageExecutors;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.file.FileNames;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.file.FileReadWrite;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * Created by caojiajun on 2025/1/14
 */
public class WalManifest implements IWalManifest {

    private static final Logger logger = LoggerFactory.getLogger(WalManifest.class);

    private static final byte[] magic_header = "camellia_header".getBytes(StandardCharsets.UTF_8);
    private static final byte[] magic_footer = "camellia_footer".getBytes(StandardCharsets.UTF_8);

    private final ConcurrentHashMap<Short, SlotWalOffset> slotWalOffsetStartMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Short, SlotWalOffset> slotWalOffsetEndMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Short, Long> slotFileMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Short, ReentrantReadWriteLock> slotLockMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> fileOffsetMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> activeWalFileMap = new ConcurrentHashMap<>();

    private final ReentrantLock lock = new ReentrantLock();

    private MappedByteBuffer mappedByteBuffer;

    private final String dir;

    public WalManifest(String dir) {
        this.dir = dir;
    }

    @Override
    public void load() throws IOException {
        FileNames.createWalManifestFileIfNotExists(dir);
        String fileName = FileNames.walManifestFile(dir);
        FileChannel fileChannel = FileChannel.open(Paths.get(fileName), StandardOpenOption.READ, StandardOpenOption.WRITE);
        int len = magic_header.length + magic_footer.length + RedisClusterCRC16Utils.SLOT_SIZE * (8+8+8) * 2;
        if (fileChannel.size() == 0) {
            ByteBuffer buffer1 = ByteBuffer.wrap(magic_header);
            while (buffer1.hasRemaining()) {
                int write = fileChannel.write(buffer1);
                logger.info("init wal.manifest.file, magic_header, key.manifest.file = {}, write.len = {}", fileName, write);
            }
            ByteBuffer buffer2 = ByteBuffer.wrap(magic_footer);
            while (buffer2.hasRemaining()) {
                int write = fileChannel.write(buffer2, magic_header.length + RedisClusterCRC16Utils.SLOT_SIZE * (8+8+8) * 2);
                logger.info("init wal.manifest.file, magic_footer, key.manifest.file = {}, write.len = {}", fileName, write);
            }
            mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, len);
        } else {
            if (fileChannel.size() != len) {
                throw new IOException("wal.manifest.file illegal size");
            }
            ByteBuffer buffer = ByteBuffer.allocate(len);
            fileChannel.read(buffer);
            buffer.flip();
            byte[] realMagicHeader = new byte[magic_header.length];
            buffer.get(realMagicHeader);
            if (!Arrays.equals(realMagicHeader, magic_header)) {
                throw new IOException("wal.manifest.file magic_header not match!");
            }
            for (short slot=0; slot<RedisClusterCRC16Utils.SLOT_SIZE; slot++) {
                long recordId = buffer.getLong();
                long fileId = buffer.getLong();
                long fileOffset = buffer.getLong();
                if (fileId > 0) {
                    slotWalOffsetStartMap.put(slot, new SlotWalOffset(recordId, fileId, fileOffset));
                    FileNames.createWalFileIfNotExists(dir, fileId);
                }
            }
            for (short slot=0; slot<RedisClusterCRC16Utils.SLOT_SIZE; slot++) {
                long recordId = buffer.getLong();
                long fileId = buffer.getLong();
                long fileOffset = buffer.getLong();
                if (fileId > 0) {
                    slotWalOffsetEndMap.put(slot, new SlotWalOffset(recordId, fileId, fileOffset));
                    slotFileMap.put(slot, fileId);
                    FileNames.createWalFileIfNotExists(dir, fileId);
                }
            }
            byte[] realMagicFooter = new byte[magic_footer.length];
            buffer.get(realMagicFooter);
            if (!Arrays.equals(realMagicFooter, magic_footer)) {
                throw new IOException("wal.manifest.file magic_footer not match!");
            }
            mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, len);
            logger.info("load wal.manifest.file success, wal.manifest.file = {}", fileName);
        }

        File dict = new File(FileNames.walFileDictionary(dir));
        if (dict.isFile()) {
            throw new IOException(FileNames.walFileDictionary(dir) + " is not dict");
        }
        File[] files = dict.listFiles();
        if (files != null) {
            for (File file : files) {
                WalFile walFile = WalFile.parse(file);
                if (walFile == null) {
                    continue;
                }
                long length = file.length();
                fileOffsetMap.put(walFile.fileId(), length);
                lockMap.computeIfAbsent(walFile.fileId(), k -> new ReentrantLock());
                if (length < LocalStorageConstants.wal_file_size) {
                    activeWalFileMap.put(walFile.fileId(), Boolean.TRUE);
                }
            }
        }
        int fileSize = walFileSize();
        if (activeWalFileMap.size() < fileSize) {
            int newFileCount = fileSize - activeWalFileMap.size();
            long time = System.currentTimeMillis();
            for (int i=0; i<newFileCount; i++) {
                long fileId = time + i;
                activeWalFileMap.put(fileId, Boolean.TRUE);
                fileOffsetMap.put(fileId, 0L);
                lockMap.computeIfAbsent(fileId, k -> new ReentrantLock());
                FileNames.createWalFileIfNotExists(dir, fileId);
            }
        }
        LocalStorageExecutors.getInstance().getWalScheduler()
                .scheduleAtFixedRate(this::schedule, 60, 60, TimeUnit.SECONDS);
    }

    @Override
    public long fileId(short slot) throws IOException {
        Long fileId = slotFileMap.get(slot);
        if (fileId != null) {
            Long offset = fileOffsetMap.get(fileId);
            if (offset < LocalStorageConstants.wal_file_size) {
                return fileId;
            } else {
                activeWalFileMap.remove(fileId);
            }
        }
        lock.lock();
        try {
            while (true) {
                if (activeWalFileMap.size() < walFileSize()) {
                    fileId = System.currentTimeMillis();
                    while (fileOffsetMap.containsKey(fileId)) {
                        fileId++;
                    }
                    fileOffsetMap.put(fileId, 0L);
                    lockMap.computeIfAbsent(fileId, k -> new ReentrantLock());
                    slotFileMap.put(slot, fileId);
                    activeWalFileMap.put(fileId, Boolean.TRUE);
                    FileNames.createWalFileIfNotExists(dir, fileId);
                    return fileId;
                } else {
                    long minOffsetFileId = -1;
                    long minOffset = Long.MAX_VALUE;
                    Set<Long> activeFileIds = new HashSet<>(activeWalFileMap.keySet());
                    for (Long activeFileId : activeFileIds) {
                        Long offset = fileOffsetMap.get(activeFileId);
                        if (offset >= LocalStorageConstants.wal_file_size) {
                            activeWalFileMap.remove(activeFileId);
                            continue;
                        }
                        if (offset < minOffset) {
                            minOffset = offset;
                            minOffsetFileId = activeFileId;
                        }
                    }
                    if (minOffsetFileId > 0) {
                        slotFileMap.put(slot, minOffsetFileId);
                        return minOffsetFileId;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ReentrantLock getWriteLock(long fileId) {
        ReentrantLock lock = lockMap.get(fileId);
        if (lock != null) return lock;
        return lockMap.computeIfAbsent(fileId, k -> new ReentrantLock());
    }

    @Override
    public long getFileWriteNextOffset(long fileId) {

        return fileOffsetMap.get(fileId);
    }

    @Override
    public void updateFileWriteNextOffset(long fileId, long nextOffset) {
        lock.lock();
        try {
            Long lastOffset = fileOffsetMap.get(fileId);
            if (nextOffset > lastOffset) {
                fileOffsetMap.put(fileId, nextOffset);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public SlotWalOffset getSlotWalOffsetEnd(short slot) {
        ReentrantReadWriteLock readWriteLock = CamelliaMapUtils.computeIfAbsent(slotLockMap, slot, k -> new ReentrantReadWriteLock());
        readWriteLock.readLock().lock();
        try {
            return slotWalOffsetEndMap.get(slot);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void updateSlotWalOffsetEnd(short slot, SlotWalOffset offset) {
        ReentrantReadWriteLock readWriteLock = CamelliaMapUtils.computeIfAbsent(slotLockMap, slot, k -> new ReentrantReadWriteLock());
        readWriteLock.writeLock().lock();
        try {
            slotWalOffsetEndMap.put(slot, offset);
            ByteBuffer buffer = ByteBuffer.allocate(24);
            buffer.putLong(offset.recordId());
            buffer.putLong(offset.fileId());
            buffer.putLong(offset.fileOffset());
            int index = magic_header.length + RedisClusterCRC16Utils.SLOT_SIZE * (8+8+8) + slot * 24;
            mappedByteBuffer.put(index, buffer.array());
            if (!slotWalOffsetStartMap.containsKey(slot)) {
                updateSlotWalOffsetStart(slot, new SlotWalOffset(0L, offset.fileId(), 0));
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void updateSlotWalOffsetStart(short slot, SlotWalOffset offset) {
        ReentrantReadWriteLock readWriteLock = CamelliaMapUtils.computeIfAbsent(slotLockMap, slot, k -> new ReentrantReadWriteLock());
        readWriteLock.writeLock().lock();
        try {
            slotWalOffsetStartMap.put(slot, offset);
            ByteBuffer buffer = ByteBuffer.allocate(24);
            buffer.putLong(offset.recordId());
            buffer.putLong(offset.fileId());
            buffer.putLong(offset.fileOffset());
            int index = magic_header.length + slot * 24;
            mappedByteBuffer.put(index, buffer.array());
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public Map<Short, SlotWalOffset> getSlotWalOffsetStartMap() {
        return slotWalOffsetStartMap;
    }

    private int walFileSize() {
        return ProxyDynamicConf.getInt("local.storage.wal.file.size", 4);
    }

    private void schedule() {
        try {
            Set<Long> walFiles = new HashSet<>(fileOffsetMap.keySet());
            long minFileId = Long.MAX_VALUE;
            for (Map.Entry<Short, SlotWalOffset> entry : slotWalOffsetStartMap.entrySet()) {
                long fileId = entry.getValue().fileId();
                walFiles.remove(fileId);
                if (fileId < minFileId) {
                    minFileId = fileId;
                }
            }
            Set<Long> toDeleteWalFiles = new HashSet<>();
            for (Long fileId : walFiles) {
                if (!activeWalFileMap.containsKey(fileId) && fileId < minFileId) {
                    toDeleteWalFiles.add(fileId);
                }
            }
            for (Long fileId : toDeleteWalFiles) {
                boolean delete = new File(FileNames.walFile(dir, fileId)).delete();
                logger.info("delete wal file, file = {}, result = {}", FileNames.walFile(dir, fileId), delete);
                fileOffsetMap.remove(fileId);
                lockMap.remove(fileId);
                FileReadWrite.getInstance().close(FileNames.walFile(dir, fileId));
            }
        } catch (Exception e) {
            logger.error("wal schedule error", e);
        }
    }
}
