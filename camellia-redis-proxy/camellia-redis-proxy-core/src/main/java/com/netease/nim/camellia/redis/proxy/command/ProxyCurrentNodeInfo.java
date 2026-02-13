package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.conf.GlobalRedisProxyEnv;
import com.netease.nim.camellia.tools.utils.InetUtils;

import java.net.InetAddress;

/**
 * Created by caojiajun on 2024/6/28
 */
public class ProxyCurrentNodeInfo {

    public static ProxyNode current() {
        String currentNodeHost = ProxyDynamicConf.getString("proxy.node.current.host", null);
        if (currentNodeHost == null || currentNodeHost.isEmpty()) {
            String ignoredInterfaces = ProxyDynamicConf.getString("proxy.node.current.host.ignored.interfaces", null);
            String preferredNetworks = ProxyDynamicConf.getString("proxy.node.current.host.preferred.interfaces", null);
            InetAddress inetAddress = InetUtils.findFirstNonLoopbackAddress(ignoredInterfaces, preferredNetworks);
            if (inetAddress == null) {
                currentNodeHost = "127.0.0.1";
            } else {
                currentNodeHost = inetAddress.getHostAddress();
            }
        }
        int announcePort = ProxyDynamicConf.getInt("proxy.node.current.announce.port", -1);
        if (announcePort > 0) {
            return new ProxyNode(currentNodeHost, announcePort, GlobalRedisProxyEnv.getCport());
        }
        return new ProxyNode(currentNodeHost, GlobalRedisProxyEnv.getPort(), GlobalRedisProxyEnv.getCport());
    }
}
