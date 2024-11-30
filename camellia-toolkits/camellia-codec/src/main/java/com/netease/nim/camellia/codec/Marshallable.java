package com.netease.nim.camellia.codec;


public interface Marshallable {

    void marshal(Pack pack);

    void unmarshal(Unpack unpack);
}
