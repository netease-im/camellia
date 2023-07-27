package com.netease.nim.camellia.codec;


public class VarIntMable implements Marshallable {
    private int data = 0;
    public VarIntMable(int _in) {
        data = _in;
    }

    public VarIntMable() {
    }

    public int getData() {
        return data;
    }

    public void marshal(Pack p) {
        p.putVarUint(data);
    }

    public void unmarshal(Unpack up) {
        data = up.popVarUint();
    }
}
