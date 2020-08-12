package com.netease.nim.camellia.redis.proxy.command.async;


import com.netease.nim.camellia.redis.proxy.command.Command;

/**
 *
 * Created by caojiajun on 2020/8/10
 */
public interface CommandInterceptor {

    /**
     *
     * @param bid 客户端传上来的bid，可能为null
     * @param bgroup 客户端传上来的bgroup，可能为null
     * @param command 命令
     * @return 拦截结果
     */
    CommandInterceptResponse check(Long bid, String bgroup, Command command);

}
