package com.netease.nim.camellia.external.call.common;


public interface CamelliaExternalCallRequestSerializer<R> {

    byte[] serialize(R t);

    R deserialize(byte[] bytes);

}
