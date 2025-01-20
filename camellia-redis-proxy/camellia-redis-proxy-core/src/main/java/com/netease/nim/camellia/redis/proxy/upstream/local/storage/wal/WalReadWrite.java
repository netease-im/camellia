package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Unpack;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.file.FileNames;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.file.FileReadWrite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2025/1/14
 */
public class WalReadWrite {

    private final FileReadWrite fileReadWrite = new FileReadWrite();

    private final String dir;

    public WalReadWrite(String dir) {
        this.dir = dir;
    }

    private String file(long fileId) {
        return FileNames.walFile(dir, fileId);
    }

    public long write(long fileId, long offset, List<LogRecord> records) throws IOException {
        Pack pack = new Pack();
        pack.putInt(0);
        pack.putInt(records.size());
        for (LogRecord record : records) {
            pack.putMarshallable(record);
        }
        int readableBytes = pack.getBuffer().readableBytes();
        pack.getBuffer().capacity(readableBytes);
        pack.replaceInt(0, readableBytes - 4);
        byte[] data = pack.getBuffer().array();
        fileReadWrite.write(file(fileId), offset, data);
        return offset + data.length;
    }

    public WalReadResult read(long fileId, long offset) throws IOException {
        int capacity = fileReadWrite.readInt(file(fileId), offset);
        if (capacity <= 0) {
            return null;
        }
        byte[] bytes = fileReadWrite.read(file(fileId), offset + 4, capacity);
        Unpack unpack = new Unpack(bytes);
        int size = unpack.popInt();
        List<LogRecord> list = new ArrayList<>(size);
        for (int i=0; i<size; i++) {
            LogRecord logRecord = new LogRecord();
            unpack.popMarshallable(logRecord);
            list.add(logRecord);
        }
        return new WalReadResult(list, offset + 4 + capacity);
    }
}
