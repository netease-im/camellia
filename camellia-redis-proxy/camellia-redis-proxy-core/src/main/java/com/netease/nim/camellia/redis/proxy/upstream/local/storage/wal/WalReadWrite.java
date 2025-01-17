package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Unpack;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.file.FileReadWrite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2025/1/14
 */
public class WalReadWrite {

    private final FileReadWrite fileReadWrite = new FileReadWrite();

    private String file(long fileId) {
        return fileId + ".wal";
    }

    public long write(long fileId, long offset, List<LogRecord> records) throws IOException {
        Pack pack = new Pack();
        pack.putInt(records.size());
        for (LogRecord record : records) {
            pack.putMarshallable(record);
        }
        pack.getBuffer().capacity(pack.getBuffer().readableBytes());
        byte[] data = pack.getBuffer().array();
        fileReadWrite.write(file(fileId), offset, data);
        return offset + data.length;
    }

    public List<LogRecord> read(long fileId, long offset) throws IOException {
        int size = fileReadWrite.readInt(file(fileId), offset);
        if (size <= 0) {
            return null;
        }
        byte[] bytes = fileReadWrite.read(file(fileId), offset + 4, size);
        Unpack unpack = new Unpack(bytes);
        List<LogRecord> list = new ArrayList<>(size);
        for (int i=0; i<size; i++) {
            LogRecord logRecord = new LogRecord();
            unpack.popMarshallable(logRecord);
        }
        return list;
    }
}
