package com.netease.nim.camellia.redis.base.resource;

import com.netease.nim.camellia.core.model.Resource;

import java.util.List;

/**
 * 格式如下：
 * 1、没有密码
 * redis-sentinel://@host:port,host:port,host:port/master
 * 2、有密码
 * redis-sentinel://password@host:port,host:port,host:port/master
 * 3、有密码且有账号
 * redis-sentinel://username:password@host:port,host:port,host:port/master
 *
 * Created by caojiajun on 2019/10/15.
 */
public class RedisSentinelResource extends Resource {
    private final String master;
    private final List<Node> nodes;
    private final String password;
    private final String userName;
    private final int db;

    public RedisSentinelResource(String master, List<Node> nodes, String userName, String password) {
        this(master, nodes, userName, password, 0);
    }

    public RedisSentinelResource(String master, List<Node> nodes, String userName, String password, int db) {
        this.master = master;
        this.nodes = nodes;
        this.password = password;
        this.userName = userName;
        this.db = db;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.RedisSentinel.getPrefix());
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
        url.append("/");
        url.append(master);
        if (db > 0) {
            url.append("?db=").append(db);
        }
        this.setUrl(url.toString());
    }

    public RedisSentinelResource(String master, List<Node> nodes, String password) {
        this(master, nodes, null, password);
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
