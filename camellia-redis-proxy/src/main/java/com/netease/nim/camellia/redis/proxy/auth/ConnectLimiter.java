package com.netease.nim.camellia.redis.proxy.auth;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.util.TenantUtils;

/**
 * 最大连接数限制，可以自定义实现ConnectLimiter的实现类，动态调整最大连接数
 * 连接数限制分为2个维度，一个是proxy的总连接数，一个是每组bid/bgroup的最大连接数
 * 当建立tcp连接时，proxy会判断当前全局连接数，如果大于等于全局最大连接数，则连接会被直接关闭
 * 当通过auth/client/hello等命令设置bid/bgroup时，proxy会判断bid/bgroup下的当前连接数，如果大于等于阈值，则会返回一个too many connects的错误，随后关闭连接
 * Created by caojiajun on 2021/10/15
 */
public class ConnectLimiter {

    private static final int DEFAULT_THRESHOLD = -1;

    /**
     * proxy支持的最大客户端连接数，默认不限制
     *
     * @return 最大连接数
     */
    public static int connectThreshold() {
        int threshold = ProxyDynamicConf.getInt("max.client.connect", DEFAULT_THRESHOLD);
        if (threshold < 0) {
            return Integer.MAX_VALUE;
        }
        return threshold;
    }

    /**
     * The maximum number of connections belonging to the bid/bgroup supported by the proxy is not limited by default
     * Return global connect threshold if bid/bgroup is not set
     *
     * @param bid    bid
     * @param bgroup bgroup
     * @return Maximum number of connections
     */
    public static int connectThreshold(long bid, String bgroup) {
        int globalThreshold = ProxyDynamicConf.getInt(TenantUtils.DEFAULT_BID + "." + TenantUtils.DEFAULT_BGROUP + ".max.client.connect", DEFAULT_THRESHOLD);
        int threshold = ProxyDynamicConf.getInt(bid + "." + bgroup + ".max.client.connect", globalThreshold);

        if (threshold < 0) {
            return Integer.MAX_VALUE;
        }
        return threshold;
    }

}
