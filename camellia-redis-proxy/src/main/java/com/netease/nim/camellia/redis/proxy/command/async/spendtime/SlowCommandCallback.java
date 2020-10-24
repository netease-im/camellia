package com.netease.nim.camellia.redis.proxy.command.async.spendtime;

import com.netease.nim.camellia.redis.proxy.command.Command;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public interface SlowCommandCallback {

    void callback(Long bid, String bgroup, Command command, double spendMillis);
}
