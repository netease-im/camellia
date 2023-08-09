package com.netease.nim.camellia.redis.base.resource;

import com.netease.nim.camellia.core.model.Resource;

import java.util.List;

/**
 * 格式如下：
 * 1、没有密码
 * rediss-cluster://@host:port,host:port,host:port
 * 2、有密码
 * rediss-cluster://password@host:port,host:port,host:port
 * 3、有密码且有账号
 * rediss-cluster://username:password@host:port,host:port,host:port
 *
 * Created by caojiajun on 2019/5/15.
 */
public class RedissClusterResource extends Resource {

    private final List<RedisClusterResource.Node> nodes;
    private final String password;
    private final String userName;

    public RedissClusterResource(List<RedisClusterResource.Node> nodes, String userName, String password) {
        this.nodes = nodes;
        this.password = password;
        this.userName = userName;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.RedissCluster.getPrefix());
        if (userName != null && password != null) {
            url.append(userName).append(":").append(password);
        } else if (userName == null && password != null) {
            url.append(password);
        }
        url.append("@");
        for (RedisClusterResource.Node node : nodes) {
            url.append(node.getHost()).append(":").append(node.getPort());
            url.append(",");
        }
        url.deleteCharAt(url.length() - 1);
        this.setUrl(url.toString());
    }

    public RedissClusterResource(List<RedisClusterResource.Node> nodes, String password) {
        this(nodes, null, password);
    }

    public List<RedisClusterResource.Node> getNodes() {
        return nodes;
    }

    public String getPassword() {
        return password;
    }

    public String getUserName() {
        return userName;
    }

}
