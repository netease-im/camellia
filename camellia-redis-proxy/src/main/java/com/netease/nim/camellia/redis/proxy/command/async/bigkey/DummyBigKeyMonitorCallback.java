package com.netease.nim.camellia.redis.proxy.command.async.bigkey;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

/**
 *
 * Created by caojiajun on 2021/4/25
 */
public class DummyBigKeyMonitorCallback implements BigKeyMonitorCallback {

    @Override
    public void callbackUpstream(Command command, byte[] key, long size, long threshold) {

    }

    @Override
    public void callbackDownstream(Command command, Reply reply, byte[] key, long size, long threshold) {

    }
}
