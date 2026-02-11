package com.netease.nim.camellia.redis.proxy.monitor;

/**
 * Created by caojiajun on 2026/2/11
 */
public interface CommandMonitor {

    void incrWrite(long bid, String bgroup, String resource, String command);

    void incrRead(long bid, String bgroup, String resource, String command);
}
