package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

import com.netease.nim.camellia.codec.Marshallable;
import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Unpack;

/**
 * Created by caojiajun on 2025/1/14
 */
public class LogRecord implements Marshallable {

    private long id;
    private short slot;
    private byte[] data;

    public LogRecord() {
    }

    public LogRecord(long id, short slot, byte[] data) {
        this.id = id;
        this.slot = slot;
        this.data = data;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public short getSlot() {
        return slot;
    }

    public void setSlot(short slot) {
        this.slot = slot;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public void marshal(Pack pack) {
        pack.putLong(id);
        pack.putShort(slot);
        pack.putVarbin(data);
    }

    @Override
    public void unmarshal(Unpack unpack) {
        id = unpack.popLong();
        slot = unpack.popShort();
        data = unpack.popVarbin();
    }
}
