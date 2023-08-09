package com.netease.nim.camellia.redis.base.resource;

import com.netease.nim.camellia.core.model.Resource;

import java.util.List;

/**
 * 格式如下：
 * 1、没有密码
 * rediss-sentinel://@host:port,host:port,host:port/master
 * 2、有密码
 * rediss-sentinel://password@host:port,host:port,host:port/master
 * 3、有密码且有账号
 * rediss-sentinel://username:password@host:port,host:port,host:port/master
 *
 * Created by caojiajun on 2019/10/15.
 */
public class RedissSentinelResource extends Resource {
    private final String master;
    private final List<RedisSentinelResource.Node> nodes;
    private final String password;
    private final String userName;
    private final int db;
    private final String sentinelUserName;
    private final String sentinelPassword;

    public RedissSentinelResource(String master, List<RedisSentinelResource.Node> nodes, String userName, String password) {
        this(master, nodes, userName, password, 0);
    }

    public RedissSentinelResource(String master, List<RedisSentinelResource.Node> nodes, String userName, String password, int db) {
        this(master, nodes, userName, password, db, null, null);
    }

    public RedissSentinelResource(String master, List<RedisSentinelResource.Node> nodes, String userName, String password, int db,
                                  String sentinelUserName, String sentinelPassword) {
        this.master = master;
        this.nodes = nodes;
        this.password = password;
        this.userName = userName;
        this.db = db;
        this.sentinelUserName = sentinelUserName;
        this.sentinelPassword = sentinelPassword;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.RedissSentinel.getPrefix());
        if (userName != null && password != null) {
            url.append(userName).append(":").append(password);
        } else if (userName == null && password != null) {
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

        boolean withParam = false;
        if (db > 0 || sentinelUserName != null || sentinelPassword != null) {
            url.append("?");
            withParam = true;
        }
        if (db > 0) {
            url.append("db=").append(db).append("&");
        }
        if (sentinelUserName != null) {
            url.append("sentinelUserName=").append(sentinelUserName).append("&");
        }
        if (sentinelPassword != null) {
            url.append("sentinelPassword=").append(sentinelPassword).append("&");
        }
        if (withParam) {
            url.deleteCharAt(url.length() - 1);
        }
        this.setUrl(url.toString());
    }

    public RedissSentinelResource(String master, List<RedisSentinelResource.Node> nodes, String password) {
        this(master, nodes, null, password);
    }

    public String getMaster() {
        return master;
    }

    public List<RedisSentinelResource.Node> getNodes() {
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

    public String getSentinelUserName() {
        return sentinelUserName;
    }

    public String getSentinelPassword() {
        return sentinelPassword;
    }
}
