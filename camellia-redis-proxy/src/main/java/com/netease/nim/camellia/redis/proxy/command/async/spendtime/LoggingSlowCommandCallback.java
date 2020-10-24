package com.netease.nim.camellia.redis.proxy.command.async.spendtime;

import com.netease.nim.camellia.redis.proxy.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class LoggingSlowCommandCallback implements SlowCommandCallback {

    private static final Logger logger = LoggerFactory.getLogger("slowCommandStats");

    @Override
    public void callback(Long bid, String bgroup, Command command, double spendMillis) {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("[");
            for (byte[] object : command.getObjects()) {
                builder.append(SafeEncoder.encode(object)).append(" ");
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append("]");
            logger.warn("slow command, bid = {}, bgroup = {}, spendMs = {}, command = {}, params = {}", bid, bgroup, spendMillis, command.getRedisCommand(), builder.toString());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
