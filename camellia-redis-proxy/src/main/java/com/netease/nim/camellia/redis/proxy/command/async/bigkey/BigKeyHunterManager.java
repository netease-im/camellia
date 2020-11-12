package com.netease.nim.camellia.redis.proxy.command.async.bigkey;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2020/11/11
 */
public class BigKeyHunterManager {

    private final ConcurrentHashMap<String, BigKeyHunter> map = new ConcurrentHashMap<>();
    private final BigKeyHunter bigKeyHunter;

    private final CommandBigKeyMonitorConfig commandBigKeyMonitorConfig;

    public BigKeyHunterManager(CommandBigKeyMonitorConfig commandBigKeyMonitorConfig) {
        this.commandBigKeyMonitorConfig = commandBigKeyMonitorConfig;
        this.bigKeyHunter = new BigKeyHunter(new CommandContext(null, null), commandBigKeyMonitorConfig);
    }

    public BigKeyHunter get(Long bid, String bgroup) {
        if (bid == null || bgroup == null) {
            return bigKeyHunter;
        } else {
            String key = bid + "|" + bgroup;
            BigKeyHunter bigKeyHunter = map.get(key);
            if (bigKeyHunter == null) {
                synchronized (BigKeyHunterManager.class) {
                    bigKeyHunter = map.get(key);
                    if (bigKeyHunter == null) {
                        bigKeyHunter = new BigKeyHunter(new CommandContext(bid, bgroup), commandBigKeyMonitorConfig);
                        map.put(key, bigKeyHunter);
                    }
                }
            }
            return bigKeyHunter;
        }
    }
}
