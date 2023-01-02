package com.netease.nim.camellia.redis.proxy.plugin;

import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

/**
 * Created by caojiajun on 2022/9/13
 */
public class ProxyPluginResponse {

    public static final ProxyPluginResponse SUCCESS = new ProxyPluginResponse(true, (Reply) null);
    public static final ProxyPluginResponse DEFAULT_FAIL = new ProxyPluginResponse(false, "ERR command request filter no pass");

    private final boolean pass;
    private final Reply reply;

    public ProxyPluginResponse(boolean pass, Reply reply) {
        this.pass = pass;
        this.reply = reply;
    }

    public ProxyPluginResponse(boolean pass, String errorMsg) {
        this.pass = pass;
        this.reply = new ErrorReply(errorMsg);
    }

    public boolean isPass() {
        return pass;
    }

    public Reply getReply() {
        return reply;
    }
}
