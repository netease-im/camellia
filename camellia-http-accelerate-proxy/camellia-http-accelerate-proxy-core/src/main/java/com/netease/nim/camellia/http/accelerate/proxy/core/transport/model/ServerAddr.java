package com.netease.nim.camellia.http.accelerate.proxy.core.transport.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by caojiajun on 2023/7/7
 */
public class ServerAddr {
    private final String host;
    private final int port;

    public ServerAddr(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public static List<ServerAddr> toAddrs(List<String> list) {
        List<ServerAddr> addrs = new ArrayList<>();
        for (String str : list) {
            addrs.add(toAddr(str));
        }
        return addrs;
    }

    public static ServerAddr toAddr(String str) {
        String[] split = str.split(":");
        return new ServerAddr(split[0], Integer.parseInt(split[1]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerAddr tcpAddr = (ServerAddr) o;
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
