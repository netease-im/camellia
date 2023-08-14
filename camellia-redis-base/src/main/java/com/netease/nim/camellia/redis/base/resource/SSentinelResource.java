package com.netease.nim.camellia.redis.base.resource;

import com.netease.nim.camellia.core.model.Resource;

import java.util.List;

/**
 * Created by caojiajun on 2023/8/14
 */
public class SSentinelResource extends Resource {

    private final List<RedisSentinelResource.Node> nodes;
    private final String sentinelUserName;
    private final String sentinelPassword;

    public SSentinelResource(List<RedisSentinelResource.Node> nodes, String sentinelUserName, String sentinelPassword) {
        this.nodes = nodes;
        this.sentinelUserName = sentinelUserName;
        this.sentinelPassword = sentinelPassword;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.SSentinel.getPrefix());
        if (sentinelUserName != null && sentinelPassword != null) {
            url.append(sentinelUserName).append(":").append(sentinelPassword);
        } else if (sentinelUserName == null && sentinelPassword != null) {
            url.append(sentinelPassword);
        }
        url.append("@");
        for (RedisSentinelResource.Node node : nodes) {
            url.append(node.getHost()).append(":").append(node.getPort());
            url.append(",");
        }
        url.deleteCharAt(url.length() - 1);
        setUrl(url.toString());
    }

    public List<RedisSentinelResource.Node> getNodes() {
        return nodes;
    }

    public String getSentinelUserName() {
        return sentinelUserName;
    }

    public String getSentinelPassword() {
        return sentinelPassword;
    }
}
