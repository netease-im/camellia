package com.netease.nim.camellia.redis.proxy.discovery.common;

import java.util.List;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2020/12/15
 */
public interface IProxySelector {
    
    Proxy next();
    //为了保持亲和性，增加了该方法，默认就是调用next方法。若需要增加亲和性，需要重载该方法
    default Proxy next(Boolean affinity){
        return next();
    } 

    void ban(Proxy proxy);

    void add(Proxy proxy);

    void remove(Proxy proxy);

    Set<Proxy> getAll();

    List<Proxy> sort(List<Proxy> list);
}
