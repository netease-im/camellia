package com.netease.nim.camellia.codec;


public class LongMable implements Marshallable {
    private long data;
    
    public LongMable(long _in) {
        data = _in;
    }

    public LongMable() {
    }

    public long getData() {
        return data;
    }

    public void marshal(Pack p) {
        p.putLong(data);
    }
    
    public void unmarshal(Unpack up) {
        data = up.popLong();
    }
}
