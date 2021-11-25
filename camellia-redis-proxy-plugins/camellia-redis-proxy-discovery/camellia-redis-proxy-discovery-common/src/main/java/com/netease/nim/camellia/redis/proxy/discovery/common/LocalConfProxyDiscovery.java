package com.netease.nim.camellia.redis.proxy.discovery.common;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by caojiajun on 2020/12/17
 */
public class LocalConfProxyDiscovery extends ProxyDiscovery {

    private final List<Proxy> list;

    /**
     *
     * @param config 10.1.1.1:6380,10.1.1.2:6380
     */
    public LocalConfProxyDiscovery(String config) {
        list = new ArrayList<>();
        String[] split = config.split(",");
        for (String str : split) {
            String[] split1 = str.split(":");
            list.add(new Proxy(split1[0], Integer.parseInt(split1[1])));
        }
    }

    public LocalConfProxyDiscovery(List<Proxy> list) {
        this.list = list;
    }

    @Override
    public List<Proxy> findAll() {
        return new ArrayList<>(list);
    }

}
