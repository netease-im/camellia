package com.netease.nim.camellia.redis.proxy.cluster;


import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

import java.util.List;
import java.util.concurrent.CompletableFuture;


public interface ClusterModeProcessor {

    /**
     * 命令是否要重定向
     * @param command Command
     * @return reply
     */
    CompletableFuture<Reply> isCommandMove(Command command);

    /**
     * cluster相关命令
     * @param command Command
     * @return reply
     */
    CompletableFuture<Reply> clusterCommands(Command command);

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

    /**
     * 获取slot-map
     * @return slot-map
     */
    ProxyClusterSlotMap getSlotMap();
}
