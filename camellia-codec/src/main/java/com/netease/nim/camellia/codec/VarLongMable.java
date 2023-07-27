package com.netease.nim.camellia.codec;


public class VarLongMable implements Marshallable {
    private long data;

    public VarLongMable(long _in) {
        data = _in;
    }

    public VarLongMable() {
    }

    public long getData() {
        return data;
    }

    public void marshal(Pack p) {
        p.putVarUlong(data);
    }
    
    public void unmarshal(Unpack up) {
        data = up.popVarUlong();
    }
}
