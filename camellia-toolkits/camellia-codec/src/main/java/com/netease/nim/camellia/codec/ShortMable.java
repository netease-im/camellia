package com.netease.nim.camellia.codec;

public class ShortMable implements Marshallable {
    private short data;
    public ShortMable(short value) {
        this.data = value;
    }

    public ShortMable() {
    }

    public short getData() {
        return data;
    }

    @Override
    public void marshal(Pack pack) {
        pack.putShort(data);
    }
    
    @Override
    public void unmarshal(Unpack unpack) {
        this.data = unpack.popShort();
    }
    
}
