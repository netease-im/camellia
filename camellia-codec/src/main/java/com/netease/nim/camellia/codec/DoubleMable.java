package com.netease.nim.camellia.codec;


public class DoubleMable implements Marshallable {
    private double data;

    public DoubleMable(double _in) {
        data = _in;
    }

    public DoubleMable() {
    }

    public double getData() {
        return data;
    }

    public void marshal(Pack p) {
        p.putDouble(data);
    }
    
    public void unmarshal(Unpack up) {
        data = up.popDouble();
    }
}
