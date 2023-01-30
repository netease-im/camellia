package com.netease.nim.camellia.redis.toolkit.lock;

/**
 *
 * Created by caojiajun on 2020/4/10.
 */
public class LockTaskResult<T> {
    private boolean execute;
    private T result;

    public LockTaskResult(boolean execute, T result) {
        this.execute = execute;
        this.result = result;
    }

    public boolean isExecute() {
        return execute;
    }

    public void setExecute(boolean execute) {
        this.execute = execute;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
