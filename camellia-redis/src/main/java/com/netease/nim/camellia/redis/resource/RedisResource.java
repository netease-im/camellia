package com.netease.nim.camellia.redis.resource;

import com.netease.nim.camellia.core.model.Resource;

/**
 * 格式如下：
 * redis://password@host:port
 *
 * Created by caojiajun on 2019/11/8.
 */
public class RedisResource extends Resource {

    private final String host;
    private final int port;
    private final String password;

    public RedisResource(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.Redis.getPrefix());
        if (password != null) {
            url.append(password);
        }
        url.append("@");
        url.append(host).append(":").append(port);
        this.setUrl(url.toString());
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
