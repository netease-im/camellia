package com.netease.nim.camellia.redis.proxy.netty;

/**
 * Created by caojiajun on 2023/7/17
 */
public interface ProxyShutdown {

    /**
     * 关闭服务器的监听，包括port和cport
     * @return 成功/失败
     */
    boolean closeServer();

    /**
     * 关闭console服务器的监听，console-port
     * @return 成功/失败
     */
    boolean closeConsoleServer();

    /**
     * 关闭服务器到redis的连接
     * @return 连接数
     */
    int closeRedisConnections();

    /**
     * 关闭所有upstream clients
     * @return 数量
     */
    int closeUpstreamClients();

    /**
     * 关闭所有upstream client templates
     * @return 数量
     */
    int closeUpstreamClientTemplates();
}
