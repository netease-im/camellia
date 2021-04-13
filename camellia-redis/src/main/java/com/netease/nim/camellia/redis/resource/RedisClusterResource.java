package com.netease.nim.camellia.redis.resource;

import com.netease.nim.camellia.core.model.Resource;

import java.util.List;

/**
 * 格式如下：
 * redis-cluster://password@host:port,host:port,host:port
 *
 * Created by caojiajun on 2019/5/15.
 */
public class RedisClusterResource extends Resource {

    private final List<Node> nodes;
    private final String password;

    public RedisClusterResource(List<Node> nodes, String password) {

        this.nodes = nodes;
        this.password = password;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.RedisCluster.getPrefix());
        if (password != null) {
            url.append(password);
        }
        url.append("@");
        for (Node node : nodes) {
            url.append(node.getHost()).append(":").append(node.getPort());
            url.append(",");
        }
        url.deleteCharAt(url.length() - 1);
        this.setUrl(url.toString());
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
