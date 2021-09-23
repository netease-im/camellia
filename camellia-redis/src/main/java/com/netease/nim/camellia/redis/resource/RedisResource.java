package com.netease.nim.camellia.redis.resource;

import com.netease.nim.camellia.core.model.Resource;

/**
 * 格式如下：
 * 1、没有密码
 * redis://@host:port
 * 2、有密码
 * redis://password@host:port
 * 3、有密码且有账号
 * redis://username:password@host:port
 *
 * Created by caojiajun on 2019/11/8.
 */
public class RedisResource extends Resource {

    private final String host;
    private final int port;
    private final String password;
    private final String userName;

    public RedisResource(String host, int port, String userName, String password) {
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.Redis.getPrefix());
        if (userName != null && password != null) {
            url.append(userName).append(":").append(password);
        } else if (userName == null && password != null) {
            url.append(password);
        }
        url.append("@");
        url.append(host).append(":").append(port);
        this.setUrl(url.toString());
    }

    public RedisResource(String host, int port, String password) {
        this(host, port, null, password);
    }

    public String getUserName() {
        return userName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

}
