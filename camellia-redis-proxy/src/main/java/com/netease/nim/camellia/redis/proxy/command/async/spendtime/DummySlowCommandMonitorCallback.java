package com.netease.nim.camellia.redis.proxy.command.async.spendtime;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

/**
 *
 * Created by caojiajun on 2021/4/25
 */
public class DummySlowCommandMonitorCallback implements SlowCommandMonitorCallback {

    @Override
    public void callback(Command command, Reply reply, double spendMillis, long thresholdMillis) {

    }
}
