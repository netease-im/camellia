package com.netease.nim.camellia.redis.proxy.cluster.provider;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMap;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;

/**
 * Created by caojiajun on 2022/9/29
 */
public interface ProxyClusterModeProvider {

    /**
     * 初始化的方法
     */
    default void init() {
        //ProxyClusterModeProcessor会调用
    }

    /**
     * 获得集群内所有proxy节点的地址列表
     * @return proxy node list
     */
    ProxyClusterSlotMap load();

    /**
     * 增加一个节点变更的回调
     * @param listener SlotMapChangeListener
     */
    void addSlotMapChangeListener(SlotMapChangeListener listener);

    /**
     * proxy间的心跳
     * @param command command
     * @return reply
     */
    default Reply proxyHeartbeat(Command command) {
        return StatusReply.OK;
    }
}
