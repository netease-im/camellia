package com.netease.nim.camellia.redis.proxy.plugin;

/**
 * Created by caojiajun on 2022/9/16
 */
public interface ProxyPluginOrder {

    ProxyPluginOrder DEFAULT = new ProxyPluginOrder() {
        @Override
        public int request() {
            return 0;
        }

        @Override
        public int reply() {
            return 0;
        }
    };

    /**
     * 请求前的plugin的排序，越大越先执行
     * @return order
     */
    default int request() {
        return 0;
    }

    /**
     * 响应前的plugin的排序，越大越先执行
     * @return order
     */
    default int reply() {
        return 0;
    }
}
