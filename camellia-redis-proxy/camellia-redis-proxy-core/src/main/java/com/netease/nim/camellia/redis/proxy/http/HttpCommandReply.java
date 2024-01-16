package com.netease.nim.camellia.redis.proxy.http;

import java.util.List;

/**
 * Created by caojiajun on 2024/1/15
 */
public class HttpCommandReply {
    private String requestId;
    private List<String> commands;
    private int code;
    private String msg;
    private List<Object> replies;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<Object> getReplies() {
        return replies;
    }

    public void setReplies(List<Object> replies) {
        this.replies = replies;
    }
}
