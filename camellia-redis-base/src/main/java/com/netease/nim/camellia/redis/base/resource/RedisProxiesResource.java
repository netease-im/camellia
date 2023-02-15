package com.netease.nim.camellia.redis.base.resource;


import com.netease.nim.camellia.core.model.Resource;

import java.util.List;

/**
 * 格式如下：
 * 1、没有密码
 * redis-proxies://@host:port,host:port,host:port
 * 2、有密码
 * redis-proxies://passwd@host:port,host:port,host:port
 * 3、有密码且有账号
 * redis-proxies://username:passwd@host:port,host:port,host:port
 * 4、有db
 * redis-proxies://username:passwd@host:port,host:port,host:port?db=1
 */
public class RedisProxiesResource extends Resource {
    private final List<Node> nodes;
    private final String password;
    private final String userName;
    private final int db;

    public RedisProxiesResource(List<Node> nodes, String userName, String password, int db) {
        this.nodes = nodes;
        this.password = password;
        this.userName = userName;
        this.db = db;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.RedisProxies.getPrefix());
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
        if (db > 0) {
            url.append("?db=").append(db);
        }
        this.setUrl(url.toString());
    }

    public RedisProxiesResource(List<Node> nodes, String userName, String password) {
        this(nodes, userName, password, 0);
    }

    public RedisProxiesResource(List<Node> nodes, String password) {
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

    public int getDb() {
        return db;
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
