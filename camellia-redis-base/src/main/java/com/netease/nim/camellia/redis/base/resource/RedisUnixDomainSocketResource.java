package com.netease.nim.camellia.redis.base.resource;

import com.netease.nim.camellia.core.model.Resource;

/**
 * 格式如下：
 * 1、没有密码
 * redis-uds://@/tmp/redis.sock
 * 2、有密码
 * redis-uds://password@/tmp/redis.sock
 * 3、有密码且有账号
 * redis-uds://user:password@/tmp/redis.sock?db=0
 * <p>
 * Created by caojiajun on 2023/10/8
 */
public class RedisUnixDomainSocketResource extends Resource {

    private final String udsPath;
    private final String userName;
    private final String password;
    private final int db;

    public RedisUnixDomainSocketResource(String udsPath, String userName, String password, int db) {
        this.udsPath = udsPath;
        this.userName = userName;
        this.password = password;
        this.db = db;
        StringBuilder url = new StringBuilder();
        url.append(RedisType.UnixDomainSocket.getPrefix());
        if (userName != null && password != null) {
            url.append(userName).append(":").append(password);
        } else if (userName == null && password != null) {
            url.append(password);
        }
        url.append("@");
        url.append(udsPath);
        if (db > 0) {
            url.append("?db=").append(db);
        }
        setUrl(url.toString());
    }

    public String getUdsPath() {
        return udsPath;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public int getDb() {
        return db;
    }
}
