package com.netease.nim.camellia.redis.proxy.cluster;

import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.util.InetUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

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
     */
    default List<ProxyNode> discovery() {
        List<ProxyNode> list = new ArrayList<>();
        list.add(current());
        return list;
    }

    /**
     * 获取当前的proxy节点地址
     */
    default ProxyNode current() {
        InetAddress inetAddress = InetUtils.findFirstNonLoopbackAddress();
        if (inetAddress == null) {
            throw new IllegalStateException("not found non loopback address");
        }
        int port = GlobalRedisProxyEnv.port;
        int cport = GlobalRedisProxyEnv.cport;
        if (port == 0 || cport == 0) {
            throw new IllegalStateException("redis proxy not start");
        }
        ProxyNode node = new ProxyNode();
        node.setHost(inetAddress.getHostAddress());
        node.setPort(port);
        node.setCport(cport);
        return node;
    }

    /**
     * 增加一个节点变更的回调
     */
    void addNodeChangeListener(ProxyNodeChangeListener listener);

    /**
     * proxy间的心跳
     */
    default Reply proxyHeartbeat(ProxyHeartbeatRequest request) {
        return StatusReply.OK;
    }
}
