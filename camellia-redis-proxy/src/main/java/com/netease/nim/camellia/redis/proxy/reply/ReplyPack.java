package com.netease.nim.camellia.redis.proxy.reply;

/**
 *
 * Created by caojiajun on 2020/8/25
 */
public class ReplyPack {

    private final Reply reply;
    private final long id;

    public ReplyPack(Reply reply, long id) {
        this.reply = reply;
        this.id = id;
    }

    public Reply getReply() {
        return reply;
    }

    public long getId() {
        return id;
    }
}
