package com.netease.nim.camellia.redis.proxy.discovery.common;

import com.netease.nim.camellia.core.discovery.ServerNode;

import java.util.List;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2020/12/15
 */
public interface IProxySelector {
    
    ServerNode next();

    //为了保持亲和性，增加了该方法，默认就是调用next方法。若需要增加亲和性，需要重载该方法
    //第一次获取affinity=true，之后每次获取affinity=false
    default ServerNode next(Boolean affinity) {
        return next();
    }

    boolean ban(ServerNode proxy);

    boolean add(ServerNode proxy);

    boolean remove(ServerNode proxy);

    Set<ServerNode> getAll();

    List<ServerNode> sort(List<ServerNode> list);
}
