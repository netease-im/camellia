package com.netease.nim.camellia.hbase.resource;

import com.netease.nim.camellia.core.model.Resource;

/**
 *
 * Created by caojiajun on 2020/3/20.
 */
public class HBaseResource extends Resource {

    public static final String prefix = "hbase://";

    private final String zk;
    private final String zkParent;
    private final String userName;
    private final String password;
    private final Boolean lindorm;

    public HBaseResource(String zk, String zkParent) {
        this(zk, zkParent, null, null);
    }

    public HBaseResource(String zk, String zkParent, String userName, String password) {
        this(zk, zkParent, userName, password, null);
    }

    public HBaseResource(String zk, String zkParent, String userName, String password, Boolean lindorm) {
        this.zk = zk;
        this.zkParent = zkParent;
        this.userName = userName;
        this.password = password;
        this.lindorm = lindorm;
        StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        builder.append(zk).append(zkParent);
        if (userName != null || password != null) {
            builder.append("?");
        }
        if (userName != null) {
            builder.append("userName=").append(userName).append("&");
        }
        if (password != null) {
            builder.append("password=").append(password).append("&");
        }
        if (lindorm != null) {
            builder.append("lindorm=").append(lindorm).append("&");
        }
        if (builder.charAt(builder.length() - 1) == '&') {
            builder.deleteCharAt(builder.length() - 1);
        }
        setUrl(builder.toString());
    }

    public String getZk() {
        return zk;
    }

    public String getZkParent() {
        return zkParent;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public boolean isLindorm() {
        return lindorm != null && lindorm;
    }
}
