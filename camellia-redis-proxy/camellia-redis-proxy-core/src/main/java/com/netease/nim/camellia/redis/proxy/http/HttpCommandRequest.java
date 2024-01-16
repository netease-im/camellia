package com.netease.nim.camellia.redis.proxy.http;

import java.util.List;

/**
 * Created by caojiajun on 2024/1/15
 */
public class HttpCommandRequest {

    private String requestId;
    private String userName;
    private String password;
    private int db = -1;
    private List<String> commands;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDb() {
        return db;
    }

    public void setDb(int db) {
        this.db = db;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }
}
