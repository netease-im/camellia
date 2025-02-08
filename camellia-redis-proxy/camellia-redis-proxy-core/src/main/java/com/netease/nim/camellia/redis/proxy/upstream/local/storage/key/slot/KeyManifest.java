package com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot;

import com.netease.nim.camellia.redis.proxy.monitor.LocalStorageCountMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.LocalStorageTimeMonitor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.LocalStorageExecutors;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.file.FileNames;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.file.FileReadWrite;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants.*;


/**
 * Created by caojiajun on 2024/12/31
 */
public class KeyManifest implements IKeyManifest {

    private static final Logger logger = LoggerFactory.getLogger(KeyManifest.class);

    private static final byte[] magic_header = "camellia_header".getBytes(StandardCharsets.UTF_8);
    private static final byte[] magic_footer = "camellia_footer".getBytes(StandardCharsets.UTF_8);

    private final ReentrantLock lock = new ReentrantLock();

    private final Map<Short, SlotInfo> slotInfoMap = new HashMap<>();//slot -> slotInfo
    private final Map<Long, BitSet> fileBitsMap1 = new ConcurrentHashMap<>();//prepare
    private final Map<Long, BitSet> fileBitsMap2 = new ConcurrentHashMap<>();//commit

    private Long activeFileId;
    private Integer activeFileOffset;

    private final String dir;
    private final String fileName;
    private FileChannel fileChannel;

    public KeyManifest(String dir) {
        this.dir = dir;
        this.fileName = FileNames.keyManifestFile(dir);
    }

    @Override
    public String dir() {
        return dir;
    }

    @Override
    public void load() throws IOException {
        logger.info("try load key.manifest.file = {}", fileName);
        FileNames.createKeyManifestFileIfNotExists(dir);
        fileChannel = FileChannel.open(Paths.get(fileName), StandardOpenOption.READ, StandardOpenOption.WRITE);
        if (fileChannel.size() == 0) {
            ByteBuffer buffer1 = ByteBuffer.wrap(magic_header);
            while (buffer1.hasRemaining()) {
                int write = fileChannel.write(buffer1);
                logger.info("init key.manifest.file, magic_header, key.manifest.file = {}, write.len = {}", fileName, write);
            }
            ByteBuffer buffer2 = ByteBuffer.wrap(magic_footer);
            while (buffer2.hasRemaining()) {
                int write = fileChannel.write(buffer2, magic_header.length + RedisClusterCRC16Utils.SLOT_SIZE * (8+8+8));
                logger.info("init key.manifest.file, magic_footer, key.manifest.file = {}, write.len = {}", fileName, write);
            }
        } else {
            int len = magic_header.length + magic_footer.length + RedisClusterCRC16Utils.SLOT_SIZE * (8+8+8);
            if (fileChannel.size() != len) {
                throw new IOException("key.manifest.file illegal size");
            }
            ByteBuffer buffer = ByteBuffer.allocate(len);
            fileChannel.read(buffer);
            buffer.flip();
            byte[] realMagicHeader = new byte[magic_header.length];
            buffer.get(realMagicHeader);
            if (!Arrays.equals(realMagicHeader, magic_header)) {
                throw new IOException("key.manifest.file magic_header not match!");
            }
            long totalCapacity = 0;
            for (short slot=0; slot<RedisClusterCRC16Utils.SLOT_SIZE; slot++) {
                long fileId = buffer.getLong();
                long offset = buffer.getLong();
                long capacity = buffer.getLong();
                if (fileId == 0) {
                    continue;
                }
                BitSet bits = fileBitsMap2.computeIfAbsent(fileId, k -> new BitSet(key_manifest_bit_size));
                int bitsStart = (int)(offset / _64k);
                int bitsEnd = (int)((offset + capacity) / _64k);
                for (int index=bitsStart; index<bitsEnd; index++) {
                    bits.set(index, true);
                }
                SlotInfo slotInfo = new SlotInfo(fileId, offset, capacity);
                slotInfoMap.put(slot, slotInfo);
                logger.info("load slot info, slot = {}, fileId = {}, offset = {}, capacity = {}", slot, fileId, offset, capacity);
                totalCapacity += capacity;
                if (activeFileId == null || fileId > activeFileId) {
                    activeFileId = fileId;
                    if (activeFileOffset == null || bitsEnd > activeFileOffset) {
                        activeFileOffset = bitsEnd;
                    }
                }
            }
            for (Map.Entry<Long, BitSet> entry : fileBitsMap2.entrySet()) {
                fileBitsMap1.put(entry.getKey(), BitSet.valueOf(entry.getValue().toLongArray()));
            }
            //
            logger.info("load slot info, key.file.count = {}, key.file.size = {}/{}",
                    fileBitsMap2.size(), totalCapacity, Utils.humanReadableByteCountBin(totalCapacity));
            logger.info("load slot info, active.file.id = {}, active.file.offset = {}", activeFileId, activeFileOffset);

            byte[] realMagicFooter = new byte[magic_footer.length];
            buffer.get(realMagicFooter);
            if (!Arrays.equals(realMagicFooter, magic_footer)) {
                throw new IOException("key.manifest.file magic_footer not match!");
            }
            logger.info("load key.manifest.file success, key.manifest.file = {}, slot.count = {}", fileName, slotInfoMap.size());
        }
        LocalStorageExecutors.getInstance().getScheduler().scheduleAtFixedRate(this::schedule, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public SlotInfo get(short slot) {
        return slotInfoMap.get(slot);
    }

    @Override
    public SlotInfo init(short slot) throws IOException {
        long time = System.nanoTime();
        try {
            lock.lock();
            try {
                SlotInfo slotInfo = slotInfoMap.get(slot);
                if (slotInfo != null) {
                    throw new IOException("slot has init");
                }
                //use active file
                if (activeFileId != null) {
                    BitSet bits = fileBitsMap1.get(activeFileId);
                    for (int i = activeFileOffset; i < key_manifest_bit_size; i++) {
                        boolean used = bits.get(i);
                        if (!used) {
                            bits.set(i, true);
                            activeFileOffset = i + 1;
                            return new SlotInfo(activeFileId, (long) i * _64k, _64k);
                        }
                    }
                }
                //new file
                long fileId = initFileId();
                fileBitsMap1.get(fileId).set(0, true);
                activeFileId = fileId;
                activeFileOffset = 1;
                return new SlotInfo(fileId, 0, _64k);
            } finally {
                lock.unlock();
            }
        } finally {
            LocalStorageTimeMonitor.time("key_slot_info_init", System.nanoTime() - time);
        }
    }

    @Override
    public SlotInfo expand(short slot, SlotInfo slotInfo) throws IOException {
        long time = System.nanoTime();
        long expandCapacity = 0;
        try {
            lock.lock();
            try {
                long capacity = slotInfo.capacity();
                if (capacity * 2 <= 0) {
                    throw new IOException("slot capacity exceed");
                }
                if (capacity * 2 > max_key_capacity) {
                    throw new IOException("slot capacity exceed");
                }
                int bitsStep = (int) (capacity / _64k);
                if (bitsStep <= 0) {
                    throw new IOException("slot capacity exceed");
                }
                //
                //use active file
                if (activeFileId != null) {
                    BitSet bits = fileBitsMap1.get(activeFileId);
                    if (activeFileOffset + 2*bitsStep < key_manifest_bit_size) {
                        boolean allocateSuccess = true;
                        int allocateOffset = activeFileOffset;
                        for (int i = activeFileOffset; i < activeFileOffset + 2*bitsStep; i++) {
                            boolean used = bits.get(i);
                            if (used) {
                                allocateSuccess = false;
                                break;
                            }
                        }
                        if (allocateSuccess) {
                            bits.set(activeFileOffset, allocateOffset + 2*bitsStep, true);
                            activeFileOffset = allocateOffset + 2*bitsStep;
                            expandCapacity = capacity*2;
                            return new SlotInfo(activeFileId, (long) allocateOffset * _64k, capacity * 2);
                        }
                    }
                }
                //new file
                long newFileId = initFileId();
                BitSet newBitSet = fileBitsMap1.get(newFileId);
                newBitSet.set(0, bitsStep*2, true);
                activeFileId = newFileId;
                activeFileOffset = bitsStep*2;
                expandCapacity = capacity*2;
                return new SlotInfo(newFileId, 0, capacity * 2);
            } finally {
                lock.unlock();
            }
        } finally {
            LocalStorageTimeMonitor.time("key_slot_info_expand_" + Utils.humanReadableByteCountBin(expandCapacity), System.nanoTime() - time);
        }
    }

    @Override
    public void commit(short slot, SlotInfo newSlotInfo, Set<SlotInfo> rollBackSlotInfos) throws IOException {
        long time = System.nanoTime();
        lock.lock();
        try {
            Set<SlotInfo> toClearList = new HashSet<>();
            if (rollBackSlotInfos != null && !rollBackSlotInfos.isEmpty()) {
                toClearList.addAll(rollBackSlotInfos);
            }

            SlotInfo oldSlotInfo = slotInfoMap.get(slot);
            if (oldSlotInfo != null && !oldSlotInfo.equals(newSlotInfo)) {
                toClearList.add(oldSlotInfo);
            }

            for (SlotInfo toClearSlotInfo : toClearList) {
                //clear bits1/bits2
                long fileId = toClearSlotInfo.fileId();
                long offset = toClearSlotInfo.offset();
                long capacity = toClearSlotInfo.capacity();
                BitSet bitSet1 = fileBitsMap1.get(fileId);
                BitSet bitSet2 = fileBitsMap2.get(fileId);
                int bitsStart = (int) (offset / _64k);
                int bitsEnd = (int) ((offset + capacity) / _64k);
                for (int i = bitsStart; i < bitsEnd; i++) {
                    bitSet1.set(i, false);
                    bitSet2.set(i, false);
                }
            }

            //
            if (oldSlotInfo != null && oldSlotInfo.equals(newSlotInfo)) {
                return;
            }
            {
                //update bits2
                long fileId = newSlotInfo.fileId();
                long offset = newSlotInfo.offset();
                long capacity = newSlotInfo.capacity();
                BitSet bitSet2 = fileBitsMap2.get(fileId);
                int bitsStart = (int) (offset / _64k);
                int bitsEnd = (int) ((offset + capacity) / _64k);
                for (int i = bitsStart; i < bitsEnd; i++) {
                    bitSet2.set(i, true);
                }
            }
            //persist
            persist(slot, newSlotInfo);
            //update cache
            slotInfoMap.put(slot, newSlotInfo);
            LocalStorageTimeMonitor.time("key_slot_info_commit_" + Utils.humanReadableByteCountBin(newSlotInfo.capacity()), System.nanoTime() - time);
        } finally {
            lock.unlock();
        }
    }

    private long initFileId() throws IOException {
        long fileId = System.currentTimeMillis();
        while (fileBitsMap1.containsKey(fileId)) {
            fileId ++;
        }
        fileBitsMap1.put(fileId, new BitSet(key_manifest_bit_size));
        fileBitsMap2.put(fileId, new BitSet(key_manifest_bit_size));
        FileNames.createKeyFile(dir, fileId);
        return fileId;
    }

    private void persist(short slot, SlotInfo slotInfo) throws IOException {
        long fileId = slotInfo.fileId();
        long offset = slotInfo.offset();
        long capacity = slotInfo.capacity();
        ByteBuffer buffer = ByteBuffer.allocate(8+8+8);
        buffer.putLong(fileId);
        buffer.putLong(offset);
        buffer.putLong(capacity);
        buffer.flip();
        long position = magic_header.length + slot * (8+8+8);
        while (buffer.hasRemaining()) {
            int write = fileChannel.write(buffer, position);
            if (logger.isDebugEnabled()) {
                logger.debug("write slot info, slot = {}, result = {}", slot, write);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("update slot info, slot = {}, fileId = {}, offset = {}, capacity = {}", slot, fileId, offset, capacity);
        }
    }

    private void schedule() {
        try {
            Set<Long> toDeleteKeyFiles = new HashSet<>();
            lock.lock();
            try {
                for (Map.Entry<Long, BitSet> entry : fileBitsMap1.entrySet()) {
                    Long fileId = entry.getKey();
                    if (Objects.equals(activeFileId, fileId)) {
                        continue;
                    }
                    int cardinality = entry.getValue().cardinality();
                    if (cardinality == 0) {
                        if (fileBitsMap2.get(fileId).cardinality() == 0) {
                            toDeleteKeyFiles.add(fileId);
                            fileBitsMap1.remove(fileId);
                            fileBitsMap2.remove(fileId);
                        }
                    }
                }
            } finally {
                lock.unlock();
            }
            if (toDeleteKeyFiles.isEmpty()) {
                return;
            }
            for (Long fileId : toDeleteKeyFiles) {
                String fileName = FileNames.keyFile(dir, fileId);
                try {
                    logger.info("try delete empty key file, file = {}", fileName);
                    FileReadWrite.getInstance().close(fileName);
                    boolean delete = new File(fileName).delete();
                    logger.info("delete empty key file, file = {}, result = {}", fileName, delete);
                    LocalStorageCountMonitor.count("key_file_delete");
                } catch (Exception e) {
                    logger.error("delete empty key file, file = {} error", fileName, e);
                }
            }
        } catch (Exception e) {
            logger.error("key file schedule error", e);
        }
    }
}
