package com.netease.nim.camellia.codec;


public class FloatMable implements Marshallable {
    private float data;

    public FloatMable(float _in) {
        data = _in;
    }

    public FloatMable() {
    }

    public float getData() {
        return data;
    }

    public void marshal(Pack p) {
        p.putFloat(data);
    }
    
    public void unmarshal(Unpack up) {
        data = up.popFloat();
    }
}
