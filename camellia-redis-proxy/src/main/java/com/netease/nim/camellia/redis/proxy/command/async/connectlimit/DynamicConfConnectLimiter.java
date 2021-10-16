package com.netease.nim.camellia.redis.proxy.command.async.connectlimit;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

/**
 * 基于ProxyDynamicConf实现的ConnectLimiter（基于camellia-redis-proxy.properties配置）
 *
 * 配置全局的最大连接数限制：
 * max.client.connect=100000
 *
 * 配置某个bid/bgroup的最大连接数限制：
 * #表示归属于bid=1,bgroup=default的最大连接数限制
 * 1.default.max.client.connect=10000
 *
 * Created by caojiajun on 2021/10/15
 */
public class DynamicConfConnectLimiter implements ConnectLimiter {

    @Override
    public int connectThreshold() {
        //默认无限制
        int threshold = ProxyDynamicConf.getInt("max.client.connect", -1);
        if (threshold < 0) {
            return Integer.MAX_VALUE;
        }
        return threshold;
    }

    @Override
    public int connectThreshold(long bid, String bgroup) {
        //默认无限制
        int threshold = ProxyDynamicConf.getInt(bid + "." + bgroup + ".max.client.connect", -1);
        if (threshold < 0) {
            return Integer.MAX_VALUE;
        }
        return threshold;
    }
}
