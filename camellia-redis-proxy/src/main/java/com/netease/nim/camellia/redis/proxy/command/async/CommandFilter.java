package com.netease.nim.camellia.redis.proxy.command.async;


import com.netease.nim.camellia.redis.proxy.command.Command;

/**
 *
 * Created by caojiajun on 2020/8/10
 */
public interface  CommandFilter {

    /**
     *
     * @param command 命令
     * @return 过滤结果
     */
    CommandFilterResponse check(Command command);

}
