package com.netease.nim.camellia.redis.proxy.sentinel;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ProxySentinelModeProcessor {


    /**
     * sentinelCommands
     * @param command command
     * @return Reply with CompletableFuture
     */
    CompletableFuture<Reply> sentinelCommands(Command command);

    /**
     * 获取当前节点
     * @return 当前节点
     */
    ProxyNode getCurrentNode();

    /**
     * 获取在线节点列表
     * @return 节点列表
     */
    List<ProxyNode> getOnlineNodes();
}
