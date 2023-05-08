package com.netease.nim.camellia.hot.key.sdk.netty;

import java.util.Objects;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyServerAddr implements Comparable<HotKeyServerAddr> {

    private final String host;
    private final int port;

    public HotKeyServerAddr(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HotKeyServerAddr that = (HotKeyServerAddr) o;
        return port == that.port && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }

    @Override
    public int compareTo(HotKeyServerAddr hotKeyServer) {
        if (hotKeyServer == null) return -1;
        return this.toString().compareTo(hotKeyServer.toString());
    }
}
