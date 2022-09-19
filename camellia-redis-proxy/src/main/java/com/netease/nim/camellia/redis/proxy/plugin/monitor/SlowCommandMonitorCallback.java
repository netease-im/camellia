package com.netease.nim.camellia.redis.proxy.plugin.monitor;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public interface SlowCommandMonitorCallback {

    /**
     * callback the slow command
     * @param command command
     * @param reply reply of the command
     * @param spendMillis spend millis
     * @param thresholdMillis config of the slow command spend millis threshold
     */
    void callback(Command command, Reply reply, double spendMillis, long thresholdMillis);
}
