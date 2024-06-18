package com.netease.nim.camellia.redis.proxy.cluster;


import java.util.Objects;

/**
 * Created by caojiajun on 2022/9/29
 */
public class ProxyNode implements Comparable<ProxyNode> {
    private final String host;
    private final int port;
    private final int cport;

    public ProxyNode(String host, int port, int cport) {
        this.host = host;
        this.port = port;
        this.cport = cport;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getCport() {
        return cport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProxyNode node = (ProxyNode) o;
        return port == node.port && cport == node.cport && Objects.equals(host, node.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, cport);
    }

    @Override
    public String toString() {
        return host + ":" + port + "@" + cport;
    }

    public static ProxyNode parseString(String str) {
        try {
            String[] split1 = str.split(":");
            String host = split1[0];
            String[] split2 = split1[1].split("@");
            int port = Integer.parseInt(split2[0]);
            int cport = Integer.parseInt(split2[1]);
            return new ProxyNode(host, port, cport);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int compareTo(ProxyNode proxyNode) {
        return toString().compareTo(proxyNode.toString());
    }
}
