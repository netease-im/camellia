package com.netease.nim.camellia.redis.proxy;

import java.util.List;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2020/12/15
 */
public interface IProxySelector {

    Proxy next();

    void ban(Proxy proxy);

    void add(Proxy proxy);

    void remove(Proxy proxy);

    Set<Proxy> getAll();

    List<Proxy> sort(List<Proxy> list);
}
