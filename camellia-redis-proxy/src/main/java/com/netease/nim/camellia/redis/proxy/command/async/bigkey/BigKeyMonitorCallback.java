package com.netease.nim.camellia.redis.proxy.command.async.bigkey;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

/**
 *
 * Created by caojiajun on 2020/11/10
 */
public interface BigKeyMonitorCallback {

    /**
     * find big key by command upstream arguments, usually by write command, such as SETEX、SADD、ZADD
     * @param commandContext commandContext, such as bid/bgroup
     * @param command command
     * @param key relevant key
     * @param size size which exceed the threshold
     * @param threshold config of big key threshold
     */
    void callbackUpstream(CommandContext commandContext, Command command, byte[] key, long size, long threshold);

    /**
     * find big key by command downstream reply, usually by read command, such as GET、SCARD、SMEMBERS、ZCARD、ZRANGE
     * @param commandContext commandContext, such as bid/bgroup
     * @param command command
     * @param reply reply of the command
     * @param key relevant key
     * @param size size which exceed the threshold
     * @param threshold config of big key threshold
     */
    void callbackDownstream(CommandContext commandContext, Command command, Reply reply, byte[] key, long size, long threshold);
}
