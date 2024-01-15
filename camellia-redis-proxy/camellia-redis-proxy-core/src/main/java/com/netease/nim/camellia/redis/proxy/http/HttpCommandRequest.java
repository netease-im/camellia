package com.netease.nim.camellia.redis.proxy.http;

import java.util.List;

/**
 * Created by caojiajun on 2024/1/15
 */
public class HttpCommandRequest {

    private String requestId;
    private List<String> commands;

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
}
