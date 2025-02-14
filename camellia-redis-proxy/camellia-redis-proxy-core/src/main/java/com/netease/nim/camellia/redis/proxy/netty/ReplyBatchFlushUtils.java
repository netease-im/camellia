package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

/**
 * Created by caojiajun on 2025/2/14
 */
public class ReplyBatchFlushUtils {

    private static boolean enable;
    private static int threshold;
    static {
        update();
        ProxyDynamicConf.registerCallback(ReplyBatchFlushUtils::update);
    }

    private static void update() {
        enable = ProxyDynamicConf.getBoolean("reply.batch.flush.enable", true);
        threshold = ProxyDynamicConf.getInt("reply.batch.flush.threshold", 16);
    }

    public static boolean enable() {
        return enable;
    }

    public static int getThreshold() {
        return threshold;
    }
}
