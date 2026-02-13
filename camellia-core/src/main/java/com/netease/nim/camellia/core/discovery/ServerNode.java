package com.netease.nim.camellia.core.discovery;

import java.util.Objects;

/**
 * Created by caojiajun on 2026/2/13
 */
public class ServerNode implements Comparable<ServerNode> {
    private String host;
    private int port;

    public ServerNode(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public ServerNode() {
    }

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ServerNode that = (ServerNode) o;
        return port == that.port && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return "InstanceNode{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }

    @Override
    public int compareTo(ServerNode node) {
        if (node == null) return -1;
        return this.toString().compareTo(node.toString());
    }
}
