package com.netease.nim.camellia.redis.proxy.http;

import java.util.List;

/**
 * Created by caojiajun on 2024/1/15
 */
public class HttpCommandRequest {

    private String requestId;
    private String userName;
    private String password;
    private Long bid;
    private String bgroup;
    private int db = -1;
    private boolean commandBase64 = false;
    private boolean replyBase64 = false;
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

    public Long getBid() {
        return bid;
    }

    public void setBid(Long bid) {
        this.bid = bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    public void setBgroup(String bgroup) {
        this.bgroup = bgroup;
    }

    public int getDb() {
        return db;
    }

    public void setDb(int db) {
        this.db = db;
    }

    public boolean isCommandBase64() {
        return commandBase64;
    }

    public void setCommandBase64(boolean commandBase64) {
        this.commandBase64 = commandBase64;
    }

    public boolean isReplyBase64() {
        return replyBase64;
    }

    public void setReplyBase64(boolean replyBase64) {
        this.replyBase64 = replyBase64;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }
}
