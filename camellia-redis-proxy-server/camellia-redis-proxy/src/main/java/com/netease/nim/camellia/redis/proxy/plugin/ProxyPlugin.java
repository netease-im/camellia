package com.netease.nim.camellia.redis.proxy.plugin;


/**
 * Created by caojiajun on 2022/9/13
 */
public interface ProxyPlugin {

    /**
     * 初始化方法
     */
    default void init(ProxyBeanFactory factory) {
    }

    /**
     * 从大到小依次执行，请求和响应可以分开定义
     * @return 优先级
     */
    default ProxyPluginOrder order() {
        return ProxyPluginOrder.DEFAULT;
    }

    /**
     * 请求（此时命令刚到proxy，还未到后端redis）
     * @param request 请求command的上下文
     */
    default ProxyPluginResponse executeRequest(ProxyRequest request) {
        return ProxyPluginResponse.SUCCESS;
    }

    /**
     * 响应（此时命令已经从后端redis响应，即将返回给客户端）
     * @param reply 响应reply上下文
     */
    default ProxyPluginResponse executeReply(ProxyReply reply) {
        return ProxyPluginResponse.SUCCESS;
    }

}
