package com.netease.nim.camellia.redis.proxy.upstream.local.storage.file;

import com.netease.nim.camellia.redis.proxy.monitor.LocalStorageMonitor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by caojiajun on 2025/1/2
 */
public class FileReadWrite {

    private final ConcurrentHashMap<String, FileChannel> fileChannelMap = new ConcurrentHashMap<>();

    private static final FileReadWrite instance = new FileReadWrite();

    private FileReadWrite() {
    }

    public static FileReadWrite getInstance() {
        return instance;
    }

    private FileChannel getFileChannel(String file) throws IOException {
        FileChannel fileChannel = fileChannelMap.get(file);
        if (fileChannel == null) {
            synchronized (fileChannelMap) {
                fileChannel = fileChannelMap.get(file);
                if (fileChannel == null) {
                    fileChannel = FileChannel.open(Paths.get(file), StandardOpenOption.READ, StandardOpenOption.WRITE);
                    fileChannelMap.put(file, fileChannel);
                }
            }
        }
        return fileChannel;
    }

    public void write(String file, long offset, byte[] data) throws IOException {
        long startTime = System.nanoTime();
        try {
            FileChannel fileChannel = getFileChannel(file);
            ByteBuffer buffer = ByteBuffer.wrap(data);

            long position = offset;
            while (buffer.hasRemaining()) {
                int write = fileChannel.write(buffer, position);
                position += write;
            }
        } finally {
            LocalStorageMonitor.fileWrite(file, data.length, System.nanoTime() - startTime);
        }
    }

    public byte[] read(String file, long offset, int size) throws IOException {
        long startTime = System.nanoTime();
        try {
            FileChannel fileChannel = getFileChannel(file);
            ByteBuffer buffer = ByteBuffer.allocate(size);
            fileChannel.read(buffer, offset);
            return buffer.array();
        } finally {
            LocalStorageMonitor.fileRead(file, size, System.nanoTime() - startTime);
        }
    }

    public int readInt(String file, long offset) throws IOException {
        FileChannel fileChannel = getFileChannel(file);
        if (offset >= fileChannel.size()) {
            return -1;
        }
        long startTime = System.nanoTime();
        try {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            fileChannel.read(buffer, offset);
            buffer.flip();
            return buffer.getInt();
        } finally {
            LocalStorageMonitor.fileRead(file, 4, System.nanoTime() - startTime);
        }
    }

    public void close(String file) throws IOException {
        FileChannel fileChannel = fileChannelMap.remove(file);
        if (fileChannel != null) {
            fileChannel.close();
        }
    }

}
