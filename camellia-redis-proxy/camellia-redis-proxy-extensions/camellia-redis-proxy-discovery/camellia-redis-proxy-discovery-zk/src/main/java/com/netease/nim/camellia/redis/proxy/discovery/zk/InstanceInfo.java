package com.netease.nim.camellia.redis.proxy.discovery.zk;


import com.netease.nim.camellia.redis.base.proxy.Proxy;

/**
 *
 * Created by caojiajun on 2020/8/12
 */
public class InstanceInfo {
    private Proxy proxy;
    private long registerTime;

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public long getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(long registerTime) {
        this.registerTime = registerTime;
    }
}
