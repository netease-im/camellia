package com.netease.nim.camellia.redis.resource;

import com.netease.nim.camellia.core.model.Resource;

import java.util.List;

/**
 * 格式如下：
 * redis-sentinel-slaves://password@host:port,host:port,host:port/masterName?withMaster=false
 * only for read
 *
 * Created by caojiajun on 2021/4/7
 */
public class RedisSentinelSlavesResource extends Resource {

    private String master;
    private List<RedisSentinelResource.Node> nodes;
    private String password;
    private boolean withMaster;

    public RedisSentinelSlavesResource(String master, List<RedisSentinelResource.Node> nodes, String password, boolean withMaster) {
        this.master = master;
        this.nodes = nodes;
        this.password = password;
        this.withMaster = withMaster;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.RedisSentinelSlaves.getPrefix());
        if (password != null) {
            url.append(password);
        }
        url.append("@");
        for (RedisSentinelResource.Node node : nodes) {
            url.append(node.getHost()).append(":").append(node.getPort());
            url.append(",");
        }
        url.deleteCharAt(url.length() - 1);
        url.append("/");
        url.append(master);
        url.append("?withMaster=").append(withMaster);
        this.setUrl(url.toString());
    }

    public String getMaster() {
        return master;
    }

    public void setMaster(String master) {
        this.master = master;
    }

    public List<RedisSentinelResource.Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<RedisSentinelResource.Node> nodes) {
        this.nodes = nodes;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isWithMaster() {
        return withMaster;
    }

    public void setWithMaster(boolean withMaster) {
        this.withMaster = withMaster;
    }

    public static class Node {
        private String host;
        private int port;

        public Node(String host, int port) {
            this.host = host;
            this.port = port;
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
    }
}
