package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.tools.utils.InetUtils;

import java.net.InetAddress;
import java.util.List;

/**
 * Created by caojiajun on 2023/12/1
 */
public interface ProxyNodesDiscovery {

    List<ProxyNode> discovery();

    default ProxyNode current() {
        String currentNodeHost = ProxyDynamicConf.getString("proxy.node.current.host", null);
        if (currentNodeHost == null) {
            InetAddress inetAddress = InetUtils.findFirstNonLoopbackAddress();
            if (inetAddress == null) {
                currentNodeHost = "127.0.0.1";
            } else {
                currentNodeHost = inetAddress.getHostAddress();
            }
        }
        return new ProxyNode(currentNodeHost, GlobalRedisProxyEnv.getPort(), GlobalRedisProxyEnv.getCport());
    }

}
