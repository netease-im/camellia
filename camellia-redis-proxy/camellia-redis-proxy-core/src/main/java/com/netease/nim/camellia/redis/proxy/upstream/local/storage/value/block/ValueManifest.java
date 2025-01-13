package com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.file.FileNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants.data_file_size;

/**
 * Created by caojiajun on 2025/1/6
 */
public class ValueManifest implements IValueManifest {

    private static final Logger logger = LoggerFactory.getLogger(ValueManifest.class);

    private final String dir;

    private final ConcurrentHashMap<Long, BlockType> typeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockType, List<Long>> fileIdMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> allocateOffsetMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, BitSet> bits1Map = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, BitSet> bits2Map = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, MappedByteBuffer> bitsMmp = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, MappedByteBuffer> slotsMmp = new ConcurrentHashMap<>();

    private final ReentrantLock lock = new ReentrantLock();

    public ValueManifest(String dir) {
        this.dir = dir;
    }

    @Override
    public String dir() {
        return dir;
    }

    @Override
    public void load() throws IOException {
        File dict = new File(dir);
        if (dict.isFile()) {
            throw new IOException(dir + " is not dict");
        }
        File[] files = dict.listFiles();
        if (files != null) {
            for (File file : files) {
                loadIndexFile(file);
            }
        }
        logger.info("value manifest load success, dir = {}, index.file.count = {}", dir, typeMap.size());
    }

    @Override
    public Map<Long, BlockType> getFileIds() {
        return new HashMap<>(typeMap);
    }

    @Override
    public BlockLocation allocate(short slot, BlockType blockType) throws IOException {
        int index = 0;
        while (true) {
            long fileId = selectFileId(blockType, index);
            BlockLocation blockLocation = allocate0(fileId);
            if (blockLocation == null) {
                index ++;
                continue;
            }
            return blockLocation;
        }
    }

    @Override
    public void commit(short slot, BlockLocation blockLocation) throws IOException {
        long fileId = blockLocation.fileId();
        int blockId = blockLocation.blockId();
        //
        BitSet bitSet1 = bits1Map.get(fileId);
        if (!bitSet1.get(blockId)) {
            throw new IOException("fileId=" + fileId + ",blockId=" + blockId + " not allocated");
        }
        //bits2
        BitSet bitSet2 = bits2Map.get(fileId);
        bitSet2.set(blockId, true);
        //update file
        //bits
        int index = blockId / 64;
        long changed = bitSet2.toLongArray()[index];
        MappedByteBuffer buffer1 = bitsMmp.get(fileId);
        buffer1.putLong(index*8, changed);
        //slot
        MappedByteBuffer buffer2 = slotsMmp.get(fileId);
        buffer2.putShort(blockId*2, slot);
    }

    @Override
    public void recycle(short slot, BlockLocation blockLocation) throws IOException {
        long fileId = blockLocation.fileId();
        int blockId = blockLocation.blockId();
        //bits1
        BitSet bitSet1 = bits1Map.get(fileId);
        if (!bitSet1.get(blockId)) {
            throw new IOException("fileId=" + fileId + ",blockId=" + blockId + " not allocated");
        }
        bitSet1.set(blockId, false);
        Integer offset = allocateOffsetMap.get(fileId);
        if (offset > blockId) {
            allocateOffsetMap.put(fileId, blockId);
        }
        //bits2
        BitSet bitSet2 = bits2Map.get(fileId);
        bitSet2.set(blockId, false);
        //update file
        int index = blockId / 64;
        long[] longArray = bitSet2.toLongArray();
        long changed;
        if (longArray.length > index) {
            changed = longArray[index];
        } else {
            changed = 0;
        }
        MappedByteBuffer buffer1 = bitsMmp.get(fileId);
        buffer1.putLong(index*8, changed);

        MappedByteBuffer buffer2 = slotsMmp.get(fileId);
        buffer2.putShort(blockId*2, (short) -1);
    }

    @Override
    public List<BlockLocation> getBlocks(short slot, BlockType blockType, int offset, int limit) {
        List<BlockLocation> list = new ArrayList<>();
        int count = 0;
        for (Map.Entry<Long, MappedByteBuffer> entry : slotsMmp.entrySet()) {
            Long fileId = entry.getKey();
            if (typeMap.get(fileId) != blockType) {
                continue;
            }
            BitSet bitSet = bits1Map.get(fileId);
            MappedByteBuffer buffer = entry.getValue();
            int size = blockType.valueSlotManifestSize(data_file_size);
            int noAllocateBlocks = 0;
            for (int i=0; i<size/2; i++) {
                if (!bitSet.get(i)) {
                    noAllocateBlocks ++;
                    if (noAllocateBlocks >= 512) {
                        break;
                    }
                    continue;
                }
                short allocateSlot = buffer.getShort(i*2);
                if (allocateSlot == slot) {
                    count ++;
                    if (count >= offset) {
                        list.add(new BlockLocation(fileId, i));
                        if (list.size() >= limit) {
                            break;
                        }
                    }
                }
            }
            if (list.size() >= limit) {
                break;
            }
        }
        return list;
    }

    @Override
    public BlockType blockType(long fileId) {
        return typeMap.get(fileId);
    }

    private BlockLocation allocate0(long fileId) {
        ReentrantLock lock = lockMap.get(fileId);
        lock.lock();
        try {
            BitSet bitSet = bits1Map.get(fileId);
            Integer start = allocateOffsetMap.get(fileId);
            for (int i=start; i<bitSet.size(); i++) {
                boolean used = bitSet.get(i);
                if (!used) {
                    bitSet.set(i, true);
                    allocateOffsetMap.put(fileId, i + 1);
                    return new BlockLocation(fileId, i);
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    private long selectFileId(BlockType blockType, int index) throws IOException {
        lock.lock();
        try {
            List<Long> list = fileIdMap.computeIfAbsent(blockType, k -> new ArrayList<>());
            if (index < list.size()) {
                return list.get(index);
            }
            long fileId = init(blockType);
            list.add(fileId);
            return fileId;
        } finally {
            lock.unlock();
        }
    }

    private long init(BlockType blockType) throws IOException {
        long fileId = System.currentTimeMillis();
        typeMap.put(fileId, blockType);
        int bitSize = (int) (data_file_size / blockType.getBlockSize());
        bits1Map.put(fileId, new BitSet(bitSize));
        bits2Map.put(fileId, new BitSet(bitSize));
        lockMap.put(fileId, new ReentrantLock());
        allocateOffsetMap.put(fileId, 0);

        String indexFileName = FileNames.createStringIndexFileIfNotExists(dir, blockType, fileId);
        String slotFileName = FileNames.createStringSlotFileIfNotExists(dir, blockType, fileId);
        FileNames.createStringDataFileIfNotExists(dir, blockType, fileId);

        {
            FileChannel fileChannel = FileChannel.open(Paths.get(indexFileName), StandardOpenOption.READ, StandardOpenOption.WRITE);
            MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, bitSize / 8);
            bitsMmp.put(fileId, map);
        }
        {
            FileChannel fileChannel = FileChannel.open(Paths.get(slotFileName), StandardOpenOption.READ, StandardOpenOption.WRITE);
            MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, bitSize * 2L);
            slotsMmp.put(fileId, map);
        }
        logger.info("init string value file, fileId = {}", fileId);
        return fileId;
    }

    private void loadIndexFile(File file) throws IOException {
        IndexFile indexFile = IndexFile.parse(file);
        if (indexFile == null) {
            return;
        }
        long fileId = indexFile.fileId();
        BlockType blockType = indexFile.blockType();
        FileChannel fileChannel = FileChannel.open(Paths.get(file.getPath()), StandardOpenOption.READ, StandardOpenOption.WRITE);
        ByteBuffer buffer = ByteBuffer.allocate((int) fileChannel.size());
        fileChannel.read(buffer);
        buffer.flip();
        long[] longArray = new long[(int) (fileChannel.size() / 8)];
        int index = 0;
        while (buffer.hasRemaining()) {
            long l = buffer.getLong();
            longArray[index] = l;
            index ++;
        }
        BitSet bitSet1 = BitSet.valueOf(longArray);
        BitSet bitSet2 = BitSet.valueOf(longArray);
        bits1Map.put(fileId, bitSet1);
        bits2Map.put(fileId, bitSet2);
        typeMap.put(fileId, blockType);
        List<Long> list = fileIdMap.computeIfAbsent(blockType, k -> new ArrayList<>());
        list.add(fileId);
        int offset = 0;
        for (int i=0; i<bitSet1.size(); i++) {
            if (!bitSet1.get(i)) {
                offset = i;
                break;
            }
        }
        allocateOffsetMap.put(fileId, offset);
        lockMap.put(fileId, new ReentrantLock());
        bitsMmp.put(fileId, fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, blockType.valueManifestSize(data_file_size)));

        String slotFile = FileNames.createStringSlotFileIfNotExists(dir, blockType, fileId);

        FileChannel slotFileChannel = FileChannel.open(Paths.get(slotFile), StandardOpenOption.READ, StandardOpenOption.WRITE);
        MappedByteBuffer map = slotFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, blockType.valueSlotManifestSize(data_file_size));
        slotsMmp.put(fileId, map);
    }
}
