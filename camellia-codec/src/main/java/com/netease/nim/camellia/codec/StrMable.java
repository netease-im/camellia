package com.netease.nim.camellia.codec;


public class StrMable implements Marshallable {
    private String data;

    public StrMable(String _in) {
        data = _in;
    }

    public StrMable() {
    }

    public String getData() {
        return data;
    }

    public void marshal(Pack p) {
        p.putVarstr(data);
    }

    public void unmarshal(Unpack up) {
        data = up.popVarstr();
    }
}
