package com.netease.nim.camellia.redis.proxy.sentinel;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by caojiajun on 2024/7/26
 */
public class DefaultSentinelModeProvider implements SentinelModeProvider {

    @Override
    public void init(ProxyNode currentNode) {
        //do nothing
    }

    @Override
    public List<ProxyNode> load() {
        String string = ProxyDynamicConf.getString("sentinel.mode.nodes", null);
        if (string == null) {
            return null;
        }
        String[] split = string.split(",");
        Set<ProxyNode> nodes = new HashSet<>();
        for (String str : split) {
            ProxyNode node = ProxyNode.parseString(str);
            if (node == null) continue;
            nodes.add(node);
        }
        return new ArrayList<>(nodes);
    }
}
