package com.netease.nim.camellia.redis.proxy.plugin.bigkey;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

/**
 *
 * Created by caojiajun on 2020/11/10
 */
public interface BigKeyMonitorCallback {

    /**
     * find big key by command's request arguments, usually by write command, such as SETEX、SADD、ZADD
     * @param command command
     * @param key relevant key
     * @param size size which exceed the threshold
     * @param threshold config of big key threshold
     */
    void callbackRequest(Command command, byte[] key, long size, long threshold);

    /**
     * find big key by command's reply, usually by read command, such as GET、SCARD、SMEMBERS、ZCARD、ZRANGE
     * @param command command
     * @param reply reply of the command
     * @param key relevant key
     * @param size size which exceed the threshold
     * @param threshold config of big key threshold
     */
    void callbackReply(Command command, Reply reply, byte[] key, long size, long threshold);
}
