package com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp.codec;

/**
 * Created by caojiajun on 2023/7/7
 */
public enum TcpPackCmd {

    REQUEST((byte) 1),
    HEARTBEAT((byte) 2),
    ;

    private final byte value;

    TcpPackCmd(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static TcpPackCmd getByValue(byte value) {
        for (TcpPackCmd cmd : TcpPackCmd.values()) {
            if (cmd.value == value) {
                return cmd;
            }
        }
        return null;
    }
}
