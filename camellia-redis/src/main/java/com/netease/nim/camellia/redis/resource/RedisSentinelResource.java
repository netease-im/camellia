package com.netease.nim.camellia.redis.resource;

import com.netease.nim.camellia.core.model.Resource;

import java.util.List;

/**
 * 格式如下：
 * redis-sentinel://password@host:port,host:port,host:port/master
 *
 * Created by caojiajun on 2019/10/15.
 */
public class RedisSentinelResource extends Resource {
    private final String master;
    private final List<Node> nodes;
    private final String password;

    public RedisSentinelResource(String master, List<Node> nodes, String password) {
        this.master = master;
        this.nodes = nodes;
        this.password = password;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.RedisSentinel.getPrefix());
        if (password != null) {
            url.append(password);
        }
        url.append("@");
        for (Node node : nodes) {
            url.append(node.getHost()).append(":").append(node.getPort());
            url.append(",");
        }
        url.deleteCharAt(url.length() - 1);
        url.append("/");
        url.append(master);
        this.setUrl(url.toString());
    }

    public String getMaster() {
        return master;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public String getPassword() {
        return password;
    }

    public static class Node {
        private final String host;
        private final int port;

        public Node(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
}
