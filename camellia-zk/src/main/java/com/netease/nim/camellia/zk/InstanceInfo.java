package com.netease.nim.camellia.zk;



/**
 *
 * Created by caojiajun on 2020/8/12
 */
public class InstanceInfo<T> {
    private T instance;
    private long registerTime;

    public T getInstance() {
        return instance;
    }

    public void setInstance(T instance) {
        this.instance = instance;
    }

    public long getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(long registerTime) {
        this.registerTime = registerTime;
    }
}
