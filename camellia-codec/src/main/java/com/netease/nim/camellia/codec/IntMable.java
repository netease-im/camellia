package com.netease.nim.camellia.codec;


public class IntMable implements Marshallable {
    private int data = 0;
    public IntMable(int _in) {
        data = _in;
    }

    public IntMable() {
    }

    public int getData() {
        return data;
    }

    public void marshal(Pack p) {
        p.putInt(data);
    }

    public void unmarshal(Unpack up) {
        data = up.popInt();
    }
}
