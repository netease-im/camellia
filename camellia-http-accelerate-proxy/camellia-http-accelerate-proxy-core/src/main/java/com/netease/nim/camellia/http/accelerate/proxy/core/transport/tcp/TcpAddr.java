package com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by caojiajun on 2023/7/7
 */
public class TcpAddr {
    private final String host;
    private final int port;

    public TcpAddr(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public static List<TcpAddr> toAddrs(List<String> list) {
        List<TcpAddr> addrs = new ArrayList<>();
        for (String str : list) {
            addrs.add(toAddr(str));
        }
        return addrs;
    }

    public static TcpAddr toAddr(String str) {
        String[] split = str.split(":");
        return new TcpAddr(split[0], Integer.parseInt(split[1]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TcpAddr tcpAddr = (TcpAddr) o;
        return port == tcpAddr.port && Objects.equals(host, tcpAddr.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
