package com.netease.nim.camellia.redis.proxy.command.async.spendtime;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public interface SlowCommandMonitorCallback {

    /**
     * callback the slow command
     * @param commandContext commandContext, such as bid/bgroup
     * @param command command
     * @param reply reply of the command
     * @param spendMillis spend millis
     * @param thresholdMillis config of the slow command spend millis threshold
     */
    void callback(CommandContext commandContext, Command command, Reply reply, double spendMillis, long thresholdMillis);
}
