package com.netease.nim.camellia.external.call.common;

/**
 * Created by caojiajun on 2023/2/24
 */
public class ExternalCallMqPack {
    private String isolationKey;
    private byte[] data;

    public String getIsolationKey() {
        return isolationKey;
    }

    public void setIsolationKey(String isolationKey) {
        this.isolationKey = isolationKey;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
