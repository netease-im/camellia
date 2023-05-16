package com.netease.nim.camellia.redis.proxy.cluster;


import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.Objects;

/**
 * Created by caojiajun on 2022/9/29
 */
public class ProxyNode implements Comparable<ProxyNode> {
    private String host;
    private int port;
    private int cport;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getCport() {
        return cport;
    }

    public void setCport(int cport) {
        this.cport = cport;
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
        return host + Utils.COLON + port + "@" + cport;
    }

    public static ProxyNode parseString(String str) {
        try {
            ProxyNode node = new ProxyNode();
            String[] split1 = str.split(Utils.COLON);
            node.setHost(split1[0]);
            String[] split2 = split1[1].split("@");
            node.setPort(Integer.parseInt(split2[0]));
            node.setCport(Integer.parseInt(split2[1]));
            return node;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int compareTo(ProxyNode proxyNode) {
        return toString().compareTo(proxyNode.toString());
    }
}
