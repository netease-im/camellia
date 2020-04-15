package com.netease.nim.camellia.redis.proxy.command.sync;

import com.netease.nim.camellia.redis.proxy.reply.Reply;
import redis.clients.jedis.Response;

/**
 *
 * Created by caojiajun on 2019/12/30.
 */
public class PipelineResponse<T> {

    private Response<T> response;
    private Transfer<T> transfer;

    private ValueLoader valueLoader;

    private Reply reply;

    public PipelineResponse(Response<T> response, Transfer<T> transfer) {
        this.response = response;
        this.transfer = transfer;
    }

    public PipelineResponse(ValueLoader valueLoader) {
        this.valueLoader = valueLoader;
    }

    public PipelineResponse(Reply reply) {
        this.reply = reply;
    }

    public Reply get() {
        if (reply != null) return reply;
        if (valueLoader != null) return valueLoader.load();
        return transfer.transfer(response);
    }

    public static interface Transfer<T> {

        Reply transfer(Response<T> response);
    }

    public static interface ValueLoader {
        Reply load();
    }
}
