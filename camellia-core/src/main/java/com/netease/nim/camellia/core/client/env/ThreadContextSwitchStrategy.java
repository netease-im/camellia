package com.netease.nim.camellia.core.client.env;

import java.util.concurrent.Callable;

/**
 * Created by caojiajun on 2022/4/6
 */
public interface ThreadContextSwitchStrategy {

    Runnable wrapperRunnable(Runnable runnable);

    <T> Callable<T> wrapperCallable(Callable<T> callable);

    public static class Default implements ThreadContextSwitchStrategy {

        @Override
        public Runnable wrapperRunnable(Runnable runnable) {
            return runnable;
        }

        @Override
        public <T> Callable<T> wrapperCallable(Callable<T> callable) {
            return callable;
        }
    }
}

