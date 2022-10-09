package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 * Created by caojiajun on 2019/12/19.
 */
public interface IAsyncCamelliaRedisTemplate {

    /**
     * 发送命令
     */
    List<CompletableFuture<Reply>> sendCommand(List<Command> commands);

    /**
     * 获取路由表
     */
    ResourceTable getResourceTable();

    /**
     * 获取路由表更新时间
     */
    long getResourceTableUpdateTime();

    /**
     * 预热，会预先建立好到后端redis的连接
     */
    void preheat();

    /**
     * 关闭
     */
    void shutdown();

    /**
     * 更新路由表
     */
    void update(ResourceTable resourceTable);
}
