package com.netease.nim.camellia.redis.proxy.upstream.local.storage.file;

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
        FileChannel fileChannel = getFileChannel(file);
        ByteBuffer buffer = ByteBuffer.wrap(data);

        long position = offset;
        while (buffer.hasRemaining()) {
            int write = fileChannel.write(buffer, position);
            position += write;
        }
    }

    public byte[] read(String file, long offset, int size) throws IOException {
        FileChannel fileChannel = getFileChannel(file);
        ByteBuffer buffer = ByteBuffer.allocate(size);
        fileChannel.read(buffer, offset);
        return buffer.array();
    }
}
