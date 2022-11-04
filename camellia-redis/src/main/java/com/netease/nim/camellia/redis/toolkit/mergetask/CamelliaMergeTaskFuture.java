package com.netease.nim.camellia.redis.toolkit.mergetask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2022/11/4
 */
public class CamelliaMergeTaskFuture<V> extends CompletableFuture<CamelliaMergeTaskResult<V>> {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaMergeTaskFuture.class);

    private final AtomicBoolean done = new AtomicBoolean(false);

    private Runnable callback;

    public CamelliaMergeTaskFuture() {
    }

    public CamelliaMergeTaskFuture(Runnable callback) {
        this.callback = callback;
    }

    @Override
    public boolean complete(CamelliaMergeTaskResult<V> value) {
        //只回调一次
        if (done.compareAndSet(false, true)) {
            if (callback != null) {
                try {
                    callback.run();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            return super.complete(value);
        }
        return false;
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        //只回调一次
        if (done.compareAndSet(false, true)) {
            if (callback != null) {
                try {
                    callback.run();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            return super.completeExceptionally(ex);
        }
        return false;
    }
}
