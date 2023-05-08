package com.netease.nim.camellia.hot.key.common.netty.codec;


public interface Marshallable {

    void marshal(Pack pack);

    void unmarshal(Unpack unpack);
}
