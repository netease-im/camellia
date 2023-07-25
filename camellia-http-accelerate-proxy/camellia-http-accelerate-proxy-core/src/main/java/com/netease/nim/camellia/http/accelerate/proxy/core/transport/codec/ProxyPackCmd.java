package com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec;

/**
 * Created by caojiajun on 2023/7/7
 */
public enum ProxyPackCmd {

    REQUEST((byte) 1),
    HEARTBEAT((byte) 2),
    ;

    private final byte value;

    ProxyPackCmd(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static ProxyPackCmd getByValue(byte value) {
        for (ProxyPackCmd cmd : ProxyPackCmd.values()) {
            if (cmd.value == value) {
                return cmd;
            }
        }
        return null;
    }
}
