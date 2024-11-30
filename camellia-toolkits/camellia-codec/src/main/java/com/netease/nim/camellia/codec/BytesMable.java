package com.netease.nim.camellia.codec;


public class BytesMable implements Marshallable {
    private byte[] data;

    public BytesMable(byte[] _in) {
        data = _in;
    }

    public BytesMable() {
    }

    public byte[] getData() {
        return data;
    }

    public void marshal(Pack p) {
        p.putVarUint(data.length);
        p.putBytes(data);
    }
    
    public void unmarshal(Unpack up) {
        int size = up.popVarUint();
        data = up.popFetch(size);
    }
}
