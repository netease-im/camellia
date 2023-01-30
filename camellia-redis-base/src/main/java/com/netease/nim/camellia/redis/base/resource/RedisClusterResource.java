package com.netease.nim.camellia.redis.base.resource;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.resource.RedisType;

import java.util.List;

/**
 * 格式如下：
 * 1、没有密码
 * redis-cluster://@host:port,host:port,host:port
 * 2、有密码
 * redis-cluster://password@host:port,host:port,host:port
 * 3、有密码且有账号
 * redis-cluster://username:password@host:port,host:port,host:port
 *
 * Created by caojiajun on 2019/5/15.
 */
public class RedisClusterResource extends Resource {

    private final List<Node> nodes;
    private final String password;
    private final String userName;

    public RedisClusterResource(List<Node> nodes, String userName, String password) {
        this.nodes = nodes;
        this.password = password;
        this.userName = userName;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.RedisCluster.getPrefix());
        if (userName != null && password != null) {
            url.append(userName).append(":").append(password);
        } else if (userName == null && password != null) {
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

    public RedisClusterResource(List<Node> nodes, String password) {
        this(nodes, null, password);
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public String getPassword() {
        return password;
    }

    public String getUserName() {
        return userName;
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
